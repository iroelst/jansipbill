import { registerPlugin } from '@capacitor/core';
export interface PairedDevice { name: string; address: string; }
export interface BluetoothClassicPlugin {
  getPairedDevices(): Promise<{ devices: PairedDevice[] }>;
  connect(options: { address: string }): Promise<{ connected: boolean; name: string; address: string }>;
  disconnect(): Promise<{ disconnected: boolean }>;
  printText(options: { text: string }): Promise<{ sent: boolean }>;
}
const BluetoothClassic = registerPlugin<BluetoothClassicPlugin>('BluetoothClassic');
export { BluetoothClassic };
