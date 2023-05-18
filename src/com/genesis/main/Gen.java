package com.genesis.main;

import javax.swing.*;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Gen {
    private static final String PREFIX = "@%#%@";
    private final BufferedReader SYS_BR;
    private boolean isOnSocket;
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private Map<String, String> envH;
    private int isInCamEnv;

    public Gen() {
        SYS_BR = new BufferedReader(new InputStreamReader(System.in));

        if (System.getenv("GENESIS_PREFIX") == null && System.getenv("INCAM_PREFIX") == null) {
            connect();
        }

        envH = getEnvironment();
    }

    private void connect() {
        try {
            socket = new Socket("127.0.0.1", 56753);
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
            isOnSocket = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isInCamEnv() {
        if (isInCamEnv == -1) {
            VOF();
            String status = COM("get_meas_ids", true);
            VON();
            if (status.equals("8008")) {
                isInCamEnv = 0;
            } else {
                isInCamEnv = 1;
            }
        }

        return isInCamEnv == 1;
    }

    private Map<String, String> getEnvironment() {
        Map<String, String> envH = new HashMap();
        if (isOnSocket) {
            sendCommand("GETENVIRONMENT", "", null);

            while (true) {
                String str = getReply().trim();
                if (str.equals("END")) {
                    break;
                }

                Pattern p = Pattern.compile("([^=]+)\\s*=\\s*(.*)");
                Matcher m = p.matcher(str);
                if (m.matches()) {
                    envH.put(m.group(1), m.group(2));
                }
            }
        } else {
            envH = System.getenv();
        }

        return envH;
    }

    private void sendCommand(String type, String command, String coding) {
        if (isOnSocket) {
            sendCommandToSocket(type, command, coding);
        } else {
            sendCommandToPipe(type, command, coding);
        }
    }

    private void sendCommandToPipe(String type, String command, String coding) {
        PrintStream out = null;
        String code;
        if (coding == null) {
            out = System.out;
        } else {
            if (coding.equals("UTF8")) {
                code = "UTF-8";
            } else {
                code = coding;
            }

            try {
                out = new PrintStream(System.out, true, code);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        out.print("@%#%@" + type + " " + command + "\n");
    }

    private void sendCommandToSocket(String type, String command, String coding) {
        try {
            dos.writeUTF("@%#%@" + type + " " + command + "\n");
            dos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getReply() {
        String reply = null;

        try {
            if (isOnSocket) {
                reply = dis.readUTF();
            } else {
                reply = SYS_BR.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return reply;
    }

    private String getReply(String coding) {
        String reply = null;

        try {
            if (isOnSocket) {
                if (coding != null && coding.equals("GBK")) {
                    reply = dis.readLine();
                } else {
                    reply = dis.readUTF();
                }
            } else {
                reply = SYS_BR.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return reply;
    }

    public Map<String, String> getEnvH() {
        return envH;
    }

    public boolean isOnSocket() {
        return isOnSocket;
    }

    public void closeSocket() {
        sendCommand("QUIT", "", null);
    }

    public void SU_ON() {
        sendCommand("SU_ON", "", null);
    }

    public void SU_OFF() {
        sendCommand("SU_OFF", "", null);
    }

    public void VON() {
        sendCommand("VON", "", null);
    }

    public void VOF() {
        sendCommand("VOF", "", null);
    }

    public String COM(String command) {
        return COM(command, null);
    }

    public String COM(String command, boolean returnStatus) {
        return COM(command, null, returnStatus);
    }

    public String COM(String command, String coding, boolean returnStatus) {
        sendCommand("COM", command, coding);
        String status = getReply(coding);
        String readans = getReply(coding);
        return returnStatus ? status : readans;
    }

    public String COM(String command, String coding) {
        sendCommand("COM", command, coding);
        getReply(coding);
        String readans = getReply(coding);
        return readans;
    }

    public String AUX(String command) {
        sendCommand("AUX", command, null);
        String status = getReply();
        String readans = getReply();
        return readans;
    }

    public String PAUSE(String command) {
        sendCommand("PAUSE", command, null);
        String status = getReply();
        String readans = getReply();
        String pauseans = getReply();
        return pauseans;
    }

    public String[] MOUSE_p(String message) {
        sendCommand("MOUSE", " p " + message, null);
        String status = getReply();
        String readans = getReply();
        String mouseans = getReply();
        return mouseans.split(" ");
    }

    public String[] MOUSE_r(String message) {
        sendCommand("MOUSE", " r " + message, null);
        String status = getReply();
        String readans = getReply();
        String mouseans = getReply();
        return mouseans.split(" ");
    }

    public String getCurrentJobName() {
        return envH.get("JOB");
    }

    public String getCurrentStepName() {
        return envH.get("STEP");
    }

    public String getWorkLayer() {
        return COM("get_work_layer");
    }

    public String[] getAffectLayers() {
        return COM("get_affect_layer").split(" ");
    }

    public String[] getDispLayers() {
        return COM("get_disp_layers").split(" ");
    }

    public HashMap<String, ArrayList<String>> INFO(String s, String unit) {
        HashMap<String, ArrayList<String>> hm = new HashMap();
        String pidname = ManagementFactory.getRuntimeMXBean().getName();
        String pid = pidname.split("@")[0];
        String tmpDir;
        String GenDir = System.getenv("GENESIS_DIR");
        if (GenDir != null) {
            tmpDir = GenDir + File.separator + "tmp";
        } else {
            tmpDir = "/tmp";
        }

        File tmpDirF = new File(tmpDir);
        if (!tmpDirF.isDirectory()) {
            File tmpDirFile = new File("D:/genesis");
            if (!tmpDirFile.exists() || !tmpDirFile.isDirectory()) {
                JOptionPane.showMessageDialog(null, tmpDir + "文件目录不存在,请找程序员！\n" + "(ERR:Genesis-00002)");
                System.exit(0);
            }
        }

        String path = tmpDir + File.separator + "info." + pid;
        String argv = "info,out_file=" + path + "," + "units=" + unit + "," + "args= -m script ";
        COM(argv + s);
        File f = new File(path);
        FileReader fr = null;
        BufferedReader br = null;

        try {
            fr = new FileReader(f);
            br = new BufferedReader(fr);

            String ts;
            while ((ts = br.readLine()) != null) {
                ts = ts.trim();
                Pattern p1 = Pattern.compile("set\\s+(\\S+)\\s*=\\s*(.*)\\s*");
                Pattern p2 = Pattern.compile("\\s*");
                Pattern p3 = Pattern.compile("\\((.*)\\)");
                Matcher m1 = p1.matcher(ts);
                Matcher m2 = p2.matcher(ts);
                if (m1.matches()) {
                    String key = m1.group(1);
                    String value = m1.group(2);
                    Matcher m3 = p3.matcher(value);
                    ArrayList al;
                    if (m3.matches()) {
                        value = m3.group(1);
                        al = shellword(value);
                        hm.put(key, al);
                    } else {
                        al = shellword(value);
                        if (al.size() == 1) {
                            hm.put(key, al);
                        } else {
                            JOptionPane.showMessageDialog(null, "info文件错误1，请找程序员！\n" + s + "\n(ERR:Genesis-00003)");
                            System.exit(0);
                        }
                    }
                } else if (!m2.matches()) {
                    JOptionPane.showMessageDialog(null, "info文件错误2，请找程序员！\n" + s + "\n(ERR:Genesis-00004)");
                    System.exit(0);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fr.close();
                br.close();
                f.delete();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return hm;
    }

    public ArrayList<String> shellword(String s) {
        String ts = s;
        ArrayList<String> c = new ArrayList();
        Pattern p1 = Pattern.compile("\\s*'(([^\"\\\\]|\\\\.)*?)'\\s*(.*)");
        Pattern p2 = Pattern.compile("\\s*\"(([^'\\\\]|\\\\.)*?)\"\\s*(.*)");
        Pattern p3 = Pattern.compile("(([^\\s\\\\'\"]|\\\\.)++)\\s*(.*)");
        Pattern p4 = Pattern.compile("\\s*");

        while (true) {
            while (true) {
                while (true) {
                    while (true) {
                        ts = ts.trim();
                        Matcher m1 = p1.matcher(ts);
                        Matcher m2 = p2.matcher(ts);
                        Matcher m3 = p3.matcher(ts);
                        Matcher m4 = p4.matcher(ts);
                        if (!m1.matches()) {
                            if (!m2.matches()) {
                                if (!m3.matches()) {
                                    if (m4.matches()) {
                                        return c;
                                    }

                                    if (ts.startsWith("'") && ts.endsWith("'")) {
                                        String[] tmp = ts.replace("' '", "<=>").replace("'", "").replace("'", "").split("<=>");
                                        String[] var16 = tmp;
                                        int var15 = tmp.length;

                                        for (int var14 = 0; var14 < var15; ++var14) {
                                            String str = var16[var14];
                                            c.add(replaceBackslash(str));
                                        }

                                        return c;
                                    }

                                    JOptionPane.showMessageDialog(null, "shellword错误，请找程序员！\n" + s + "\n(ERR:Genesis-00005)");
                                    System.exit(0);
                                } else {
                                    ts = m3.group(3);
                                    c.add(replaceBackslash(m3.group(1)));
                                }
                            } else {
                                ts = m2.group(3);
                                c.add(replaceBackslash(m2.group(1)));
                            }
                        } else {
                            ts = m1.group(3);
                            c.add(replaceBackslash(m1.group(1)));
                        }
                    }
                }
            }
        }
    }

    private static String replaceBackslash(String s) {
        String ts = s;
        String rs = "";
        Pattern p1 = Pattern.compile("(.*?)\\\\(.)(.*)");

        while (true) {
            Matcher m1 = p1.matcher(ts);
            if (!m1.matches()) {
                rs = rs + ts;
                return rs;
            }

            ts = m1.group(3);
            rs = rs + m1.group(1) + m1.group(2);
        }
    }
}
