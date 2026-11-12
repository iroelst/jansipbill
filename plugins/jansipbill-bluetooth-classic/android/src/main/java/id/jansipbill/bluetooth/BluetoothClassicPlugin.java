package id.jansipbill.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Build;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.UUID;

@CapacitorPlugin(
    name = "BluetoothClassic",
    permissions = {
        @Permission(alias = "bluetoothConnect", strings = { Manifest.permission.BLUETOOTH_CONNECT })
    }
)
public class BluetoothClassicPlugin extends Plugin {
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothSocket socket;
    private BluetoothDevice connectedDevice;

    private boolean ensureBluetoothPermission(PluginCall call, String callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            getPermissionState("bluetoothConnect") != PermissionState.GRANTED) {
            requestPermissionForAlias("bluetoothConnect", call, callback);
            return false;
        }
        return true;
    }

    @PluginMethod
    public void getPairedDevices(PluginCall call) {
        if (!ensureBluetoothPermission(call, "pairedPermissionCallback")) return;
        listPaired(call);
    }

    @PermissionCallback
    private void pairedPermissionCallback(PluginCall call) {
        listPaired(call);
    }

    private void listPaired(PluginCall call) {
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null) { call.reject("Perangkat ini tidak memiliki Bluetooth."); return; }
            if (!adapter.isEnabled()) { call.reject("Aktifkan Bluetooth Android terlebih dahulu."); return; }
            Set<BluetoothDevice> bonded = adapter.getBondedDevices();
            JSArray devices = new JSArray();
            for (BluetoothDevice device : bonded) {
                JSObject item = new JSObject();
                item.put("name", device.getName() == null ? "Bluetooth device" : device.getName());
                item.put("address", device.getAddress());
                devices.put(item);
            }
            JSObject result = new JSObject();
            result.put("devices", devices);
            call.resolve(result);
        } catch (SecurityException e) {
            call.reject("Izin Bluetooth belum diberikan.", e);
        }
    }

    @PluginMethod
    public void connect(PluginCall call) {
        if (!ensureBluetoothPermission(call, "connectPermissionCallback")) return;
        connectDevice(call);
    }

    @PermissionCallback
    private void connectPermissionCallback(PluginCall call) {
        connectDevice(call);
    }

    private void connectDevice(PluginCall call) {
        String address = call.getString("address");
        if (address == null || address.trim().isEmpty()) { call.reject("Alamat printer tidak valid."); return; }
        new Thread(() -> {
            try {
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                if (adapter == null || !adapter.isEnabled()) { call.reject("Bluetooth tidak aktif."); return; }
                if (socket != null) { try { socket.close(); } catch (Exception ignored) {} socket = null; }
                BluetoothDevice device = adapter.getRemoteDevice(address);
                // The device is already paired. Do not call cancelDiscovery() here:
                // on Android 12+ it also requires BLUETOOTH_SCAN and can fail even
                // when Nearby Devices / BLUETOOTH_CONNECT is granted.
                BluetoothSocket newSocket = null;
                Exception secureError = null;
                try {
                    newSocket = device.createRfcommSocketToServiceRecord(SPP_UUID);
                    newSocket.connect();
                } catch (Exception e) {
                    secureError = e;
                    try { if (newSocket != null) newSocket.close(); } catch (Exception ignored) {}
                    // Some low-cost ESC/POS printers expose SPP only through an
                    // insecure RFCOMM socket. Try that as a compatibility fallback.
                    newSocket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID);
                    newSocket.connect();
                }
                socket = newSocket;
                connectedDevice = device;
                JSObject result = new JSObject();
                result.put("connected", true);
                result.put("name", device.getName() == null ? "Bluetooth printer" : device.getName());
                result.put("address", address);
                call.resolve(result);
            } catch (Exception e) {
                String detail = e.getClass().getSimpleName() + (e.getMessage() == null ? "" : ": " + e.getMessage());
                call.reject("Koneksi Bluetooth SPP gagal (" + detail + "). Pastikan printer menyala, tidak terhubung ke HP lain, dan mendukung Bluetooth Classic SPP.", e);
            }
        }).start();
    }

    @PluginMethod
    public void disconnect(PluginCall call) {
        try {
            if (socket != null) socket.close();
            socket = null;
            connectedDevice = null;
            JSObject result = new JSObject();
            result.put("disconnected", true);
            call.resolve(result);
        } catch (Exception e) {
            call.reject("Gagal memutus koneksi printer.", e);
        }
    }

    @PluginMethod
    public void printText(PluginCall call) {
        String text = call.getString("text");
        if (text == null) { call.reject("Teks cetak kosong."); return; }
        new Thread(() -> {
            try {
                if (socket == null || !socket.isConnected()) { call.reject("Printer belum terhubung."); return; }
                OutputStream out = socket.getOutputStream();
                // RPP02N / ESC-POS text mode. CP437 covers basic Latin characters;
                // callers should keep receipts simple and avoid unsupported glyphs.
                out.write(text.getBytes(Charset.forName("CP437")));
                out.flush();
                JSObject result = new JSObject();
                result.put("sent", true);
                call.resolve(result);
            } catch (Exception e) {
                call.reject("Gagal mengirim data ke printer.", e);
            }
        }).start();
    }
}
