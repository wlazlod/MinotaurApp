package pl.edu.pw.meil.knr.dwlazlo.minotaur;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    TextView txtArduino, txtString, txtStringLength;
    Handler bluetoothIn;

    Button btnOn, btnOff, btnDis, btnRst;
    TextView senLeFr, senLeBa, senRiFr, senRiBa, senFrLe, senFrRi, angle, distance;

    Button btnLef, btnRig, btnFor;

    final int handlerState = 0;                        //used to identify handler message
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder recDataString = new StringBuilder();

    private ConnectedThread mConnectedThread;

    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // String for MAC address
    private static String address;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        //Link the buttons and textViews to respective views
        txtString = (TextView) findViewById(R.id.txtString);
        txtStringLength = (TextView) findViewById(R.id.testView1);

        btnOn = (Button)findViewById(R.id.button2);
        btnOff = (Button)findViewById(R.id.button3);
        btnDis = (Button)findViewById(R.id.button4);
        btnRst = (Button)findViewById(R.id.button5);

        btnLef = (Button)findViewById(R.id.button6);
        btnRig = (Button)findViewById(R.id.button7);
        btnFor = (Button)findViewById(R.id.button);

        senLeFr = (TextView)findViewById(R.id.textView5);
        senLeBa = (TextView)findViewById(R.id.textView6);
        senFrLe = (TextView)findViewById(R.id.textView3);
        senFrRi = (TextView)findViewById(R.id.textView4);
        senRiFr = (TextView)findViewById(R.id.textView8);
        senRiBa = (TextView)findViewById(R.id.textView7);
        angle = (TextView)findViewById(R.id.textView9);
        distance = (TextView)findViewById(R.id.textView10);

        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {                                     //if message is what we want
                    String readMessage = (String) msg.obj;                                                                // msg.arg1 = bytes from connect thread
                    recDataString.append(readMessage);                                      //keep appending to string until ~
                    int endOfLineIndex = recDataString.indexOf("~");                    // determine the end-of-line
                    if (endOfLineIndex > 0) {                                           // make sure there data before ~
                        String dataInPrint = recDataString.substring(0, endOfLineIndex);    // extract string
                        txtString.setText("Otrzymane dane = " + dataInPrint);
                        int dataLength = dataInPrint.length();                          //get length of data received
                        txtStringLength.setText("Dlugosc stringa = " + String.valueOf(dataLength));

                                if (recDataString.charAt(0) == '#')                             //if it starts with # we know it is what we are looking for
                                {
                                    String sensor0 = recDataString.substring(1, 5);             //get sensor value from string between indices 1-5
                                    String sensor1 = recDataString.substring(5, 9);            //same again...
                                    String sensor2 = recDataString.substring(9, 13);
                                    String sensor3 = recDataString.substring(13, 17);
                                    String sensor4 = recDataString.substring(17, 21);
                                    String sensor5 = recDataString.substring(21, 25);
                                    String sensor6 = recDataString.substring(25, 29);
                                    String sensor7 = recDataString.substring(29, 33);

                                    senLeFr.setText(sensor2);    //update the textviews with sensor values
                                    senLeBa.setText(sensor0);
                                    senFrLe.setText(sensor4);
                                    senFrRi.setText(sensor5);
                                    senRiFr.setText(sensor3);
                                    senRiBa.setText(sensor1);
                                    angle.setText("Angle error: " + sensor6);
                                    distance.setText("Distance: " + sensor7);
                                }
                                recDataString.delete(0, recDataString.length());                    //clear all string data
                                // strIncom =" ";
                                dataInPrint = " ";
                        }
                }
            }
        };

        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        checkBTState();

        // Set up onClick listeners for buttons to send 1 or 0 to turn on/off LED
        btnOff.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mConnectedThread.write("p");    // Send "0" via Bluetooth
                Toast.makeText(getBaseContext(), "Please, stahp!", Toast.LENGTH_SHORT).show();
            }
        });

        btnOn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mConnectedThread.write("d");    // Send "1" via Bluetooth
                Toast.makeText(getBaseContext(), "Go, go, go", Toast.LENGTH_SHORT).show();
            }
        });

        btnRst.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mConnectedThread.write("r");    // Send "1" via Bluetooth
                Toast.makeText(getBaseContext(), "One more time.", Toast.LENGTH_SHORT).show();
            }
        });

        btnDis.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mConnectedThread.disconnect();
                Toast.makeText(getBaseContext(), "Shhhh, only dreams now.", Toast.LENGTH_SHORT).show();
            }
        });

        btnRig.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mConnectedThread.write("R");    // Send "1" via Bluetooth
                Toast.makeText(getBaseContext(), "Korwin Power", Toast.LENGTH_SHORT).show();
            }
        });

        btnLef.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mConnectedThread.write("L");    // Send "1" via Bluetooth
                Toast.makeText(getBaseContext(), "To the left, to the left", Toast.LENGTH_SHORT).show();
            }
        });

        btnFor.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mConnectedThread.write("P");    // Send "1" via Bluetooth
                Toast.makeText(getBaseContext(), "Go ahead", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {

        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connecetion with BT device using UUID
    }

    @Override
    public void onResume() {
        super.onResume();

        //Get MAC address from DeviceListActivity via intent
        Intent intent = getIntent();

        //Get the MAC address from the DeviceListActivty via EXTRA
        address = intent.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);

        //create device and set the MAC address
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_LONG).show();
        }
        // Establish the Bluetooth socket connection.
        try
        {
            btSocket.connect();
        } catch (IOException e) {
            try
            {
                btSocket.close();
            } catch (IOException e2)
            {
                //insert code to deal with this
            }
        }
        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();

        //I send a character when resuming.beginning transmission to check device is connected
        //If it is not an exception will be thrown in the write method and finish() will be called
        mConnectedThread.write("x");
    }

    @Override
    public void onPause()
    {
        super.onPause();
        try
        {
            //Don't leave Bluetooth sockets open when leaving activity
            btSocket.close();
        } catch (IOException e2) {
            //insert code to deal with this
        }
    }

    //Checks that the Android device Bluetooth is available and prompts to be turned on if off
    private void checkBTState() {

        if(btAdapter==null) {
            Toast.makeText(getBaseContext(), "Device does not support bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (btAdapter.isEnabled()) {
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    //create new class for connect thread
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //creation of the connect thread
        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);            //read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    // Send the obtained bytes to the UI Activity via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }
        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Toast.makeText(getBaseContext(), "Connection Failure", Toast.LENGTH_LONG).show();
                finish();

            }
        }


        //disconnect method
        public void disconnect() {
            try {
                mmInStream.close();
                mmOutStream.close();
            } catch (IOException e) {
                //if you cannot write, close the application
                Toast.makeText(getBaseContext(), "Coś nie pykło", Toast.LENGTH_LONG).show();
                finish();

            }
        }
    }
}