/**
 * RS232 Utils
 * @author caisenchuan@163.com
 * */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.comm.CommDriver;
import javax.comm.CommPortIdentifier;
import javax.comm.NoSuchPortException;
import javax.comm.PortInUseException;
import javax.comm.SerialPort;
import javax.comm.UnsupportedCommOperationException;

public class RS232 {
    private String PortName;  
    private CommPortIdentifier portId;  
    private SerialPort serialPort;  
    private OutputStream out;
    private InputStream in;
    
    /**
     * Load libs
     * @return
     */
    static {
        try {
            System.loadLibrary("win32com");
            System.out.println("loadLibrary()...win32com.dll");
            String driverName = "com.sun.comm.Win32Driver";
            CommDriver driver = (CommDriver) Class.forName(driverName).newInstance();
            driver.initialize();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    } 
    
    /**
     * Initialize
     * @return
     */
    public int open(int PortID) {
        int openSignal = 1;
        
        PortName = "COM" + PortID;
        System.out.println("Open port : " + PortName);
        try {
            portId = CommPortIdentifier.getPortIdentifier(PortName);
            try {
                serialPort = (SerialPort) portId.open("Serial_Communication", 2000);
                System.out.println("Open Serial success!");
            } catch (PortInUseException e) {
                System.out.println("error : " + e);
                e.printStackTrace();
                
                if (!portId.getCurrentOwner().equals("Serial_Communication")) {
                    openSignal = 2;
                } else if (portId.getCurrentOwner().equals("Serial_Communication")) {
                    openSignal = 1;
                    return openSignal;
                }
                
                return openSignal;
            }

            // Use InputStream in to read from the serial port, and OutputStream
            // out to write to the serial port.
            try {
                in = serialPort.getInputStream();
                out = serialPort.getOutputStream();
                System.out.println("Open stream success!");
            } catch (IOException e) {
                openSignal = 3;
                e.printStackTrace();
                return openSignal;
            }

            // Initialize the communication parameters to 9600, 8, 1, none.
            try {
                serialPort.setSerialPortParams(9600, 
                                               SerialPort.DATABITS_8,
                                               SerialPort.STOPBITS_1, 
                                               SerialPort.PARITY_NONE);
                System.out.println("Set params success!");
            } catch (UnsupportedCommOperationException e) {
                openSignal = 4;
                e.printStackTrace();
                return openSignal;
            }
        } catch (NoSuchPortException e) {
            portId = null;
            openSignal = 5;
            e.printStackTrace();
            return openSignal;
        }

        // when successfully open the serial port, create a new serial buffer,
        // then create a thread that consistently accepts incoming signals from
        // the serial port. Incoming signals are stored in the serial buffer.

        // return success information
        
        return openSignal;
    }

    /**
     * Write data to RS232
     * @param Msg
     */
    public void write(String Msg) {
        System.out.println("WritePort : " + Msg);
        try {
            if (out != null) {
                for (int i = 0; i < Msg.length(); i++) {
                    out.write(Msg.charAt(i));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    /**
     * Read data from RS232
     */
    public void read() {
        int c;
        try {
            if (in != null) {
                while (in.available() > 0) {
                    c = in.read();
                    Character d = new Character((char) c);
                    System.out.print(d.toString() + ", ");
                }
            }
            
            System.out.println("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /** 
    * Close RS232
    */  
    public void close() {
        if(serialPort != null) {
            serialPort.close();  
        }
    }
    
    /**
     * Test function
     * @param args
     */
    public static void main(String[] args) {
        int PORT = 32;
        RS232 test = new RS232();
        test.open(PORT);
        
        int cnt = 0;
        while(true) {
            //String str = String.format("0,0,%s,0,0", cnt);
            String str = String.format("0,%s,0,0", cnt * 10);
            test.write(str);
            if(cnt < 10) {
                cnt ++;
            } else {
                break;
            }
            
            test.read();
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        test.close();
        
        // Keep this process running until Enter is pressed
        System.out.println("Press Enter to quit...");
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        System.out.println("Exit");
    }
}
