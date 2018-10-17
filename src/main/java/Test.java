import java.awt.*;
import java.awt.event.*;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.*;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.alidns.model.v20150109.*;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;

import javax.swing.*;
import javax.xml.soap.Text;

public class Test extends JFrame {

    private static IAcsClient client = null;

    private static String PPPIP = null;

    static Test mf = null;
    static SystemTray tray = SystemTray.getSystemTray();

    private JTextArea textArea = null;
    private JLabel jl1 = null;

    //初始化client
    static {
        String regionId = "cn-hangzhou"; //域名SDK请使用固定值"cn-hangzhou"
        String accessKeyId = ""; // your accessKey
        String accessKeySecret = "";// your accessSecret
        IClientProfile profile = DefaultProfile.getProfile(regionId, accessKeyId, accessKeySecret);
        // 若报Can not find endpoint to access异常，请添加以下此行代码
        // DefaultProfile.addEndpoint("cn-hangzhou", "cn-hangzhou", "Domain", "domain.aliyuncs.com");
        client = new DefaultAcsClient(profile);
    }

    public Test(){
        this.setTitle("内网穿透");
        this.setSize(500,400);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setResizable(false);
        this.setIconImage(new ImageIcon(Text.class.getResource("/image/ico.png")).getImage());
        mf = this;
        Container mainP = this.getContentPane();
        mainP.setLayout(new FlowLayout());

        Panel p = new Panel(new BorderLayout());
        Panel p1 = new Panel();
        JLabel jl = new JLabel("服务器域名：你的域名");
        jl1 = new JLabel("本机公网ip："+PPPIP);
        p1.add(jl);
        p1.add(jl1);
        p.add(p1,BorderLayout.NORTH);

        Panel p2 = new Panel();
        textArea = new JTextArea();
        textArea.setColumns(43);
        textArea.setRows(19);
        textArea.setLineWrap(true);
        textArea.setEditable(false);
        JScrollPane jp = new JScrollPane();
        jp.setViewportView(textArea);
        Dimension dime = textArea.getPreferredSize();
        jp.setBounds(0,0,dime.width,dime.height);
        p2.add(jp);
        p.add(p2,BorderLayout.CENTER);

        Panel p3 = new Panel();
        JLabel jLabel = new JLabel("开发者：CJH");
        p3.add(jLabel);
        p.add(p3,BorderLayout.SOUTH);
        mainP.add(p);

        miniTray(); // 显示托盘


        this.enableEvents(AWTEvent.WINDOW_EVENT_MASK);

        this.addWindowListener(new WindowAdapter() { // 窗口关闭事件
            public void windowClosing(WindowEvent e) {
                mf.setVisible(false);
             };

            public void windowIconified(WindowEvent e) { // 窗口最小化事件
                //mf.setVisible(false);
            }

        });

        this.setVisible(true);
    }

    @Override
    protected void processWindowEvent(WindowEvent e) {
        if (e.getID() == WindowEvent.WINDOW_CLOSING){
            mf.setVisible(false);
            return; //直接返回，阻止默认动作，阻止窗口关闭
        }
        super.processWindowEvent(e);
    }

    private void miniTray() {
        ImageIcon trayImg = new ImageIcon(Text.class.getResource("/image/ico.png"));//托盘图标

        PopupMenu pop = new PopupMenu(); //增加托盘右击菜单
        MenuItem show = new MenuItem("还原");
        MenuItem exit = new MenuItem("退出");

        pop.add(show);
        pop.add(exit);

        TrayIcon trayIcon = new TrayIcon(trayImg.getImage(), "内网穿透", pop);
        trayIcon.setImageAutoSize(true);

        trayIcon.addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent e) { // 鼠标器双击事件

                if(e.getButton() == e.BUTTON1)
                {
                    // 左键
                    mf.setVisible(true);
                    mf.setExtendedState(JFrame.NORMAL); // 还原窗口
                    mf.toFront();
                }

            }
        });

        exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        show.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mf.setVisible(true);
                mf.setExtendedState(JFrame.NORMAL); // 还原窗口
                mf.toFront();
            }
        });

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            e.printStackTrace();
        }


    }

    static void print(String con){
        Date date = new Date();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println(df.format(date)+"  "+con);
        mf.textArea.append(df.format(date)+"  "+con+"\r\n");
    }

    public static void main(String[] args) {
        //初始化请求

        DescribeDomainRecordsRequest describeDomainRecordsRequest = new DescribeDomainRecordsRequest();
        describeDomainRecordsRequest.setDomainName("你的域名");

        DescribeDomainRecordInfoRequest infoRequest = new DescribeDomainRecordInfoRequest();
        infoRequest.setRecordId("你的云解析id");
        //发起api调用并解析结果
        try {

            //另一种方式, 通过调用getAcsResponse方法, 获取反序列化后的对象, 示例代码如下:
            DescribeDomainRecordInfoResponse response = client.getAcsResponse(infoRequest);

            // 第一次加载获取云解析ip
            PPPIP = response.getValue();

        } catch (ServerException e) {
            e.printStackTrace();
        } catch (ClientException e) {
            e.printStackTrace();
        }
        mf = new Test();
        print("加载中...");

        print("云解析ip："+PPPIP);

        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    print("\r\n");
                    Test.run();

                    try {
                        Thread.sleep(10 * 60 * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

    }

    static void run(){
        // 监听公网ip

        print("正在获取本地公网Ip...");
        String ip = getIP();  // 获取本地公网Ip；
        if(ip != null) {
            print("获取本地公网Ip成功："+ip);
            if (!PPPIP.equals(ip)) {
                // 如果解析ip和本地公网ip不一致则更新云解析
                print("正在更新云解析...");
                if (updateIp(ip)) {
                    print("更新公网ip成功: "+PPPIP);
                    mf.jl1.setText("本机公网ip："+PPPIP);
                }else{
                    print("更新公网ip失败，十分钟后重试！");
                }
            }else{
                print("本次不更新");
            }
        }else{
            print("获取本地公网ip失败，稍后重试！");
        }


    }

    static boolean updateIp(String ip){
        UpdateDomainRecordRequest request = new UpdateDomainRecordRequest();
        request.setActionName("UpdateDomainRecord");
        request.setRecordId("你的云解析id");
        request.setType("A");
        request.setRR("lo");
        request.setValue(ip);
        try {
            UpdateDomainRecordResponse response = client.getAcsResponse(request);
            if(response.getRecordId().equals("你的云解析id")){
                PPPIP = ip;
                return true;
            }
        } catch (ClientException e) {
            e.printStackTrace();
        }
        return false;
    }

    static String getIP(){
        try {
            //获取所有接口，并放进枚举集合中，然后使用Collections.list()将枚举集合转换为ArrayList集合
            Enumeration<NetworkInterface> enu = NetworkInterface.getNetworkInterfaces();
            ArrayList<NetworkInterface> arr = Collections.list(enu);
            for(Iterator<NetworkInterface> it = arr.iterator(); it.hasNext();) {
                NetworkInterface ni = it.next();
                String intName = ni.getName(); //获取接口名
                String displayName = ni.getDisplayName();
                if(!intName.equals("ppp2")){
                    continue;
                }
                //获取每个接口中的所有ip网络接口集合，因为可能有子接口
                ArrayList<InetAddress> inets = Collections.list(ni.getInetAddresses());
                for(Iterator<InetAddress> it1 = inets.iterator();it1.hasNext();) {
                    InetAddress inet = it1.next();
                    //只筛选ipv4地址，否则会同时得到Ipv6地址
                    if(inet instanceof Inet4Address) {
                        String ip = inet.getHostAddress();
                        return ip;

                    }
                }
            }
        } catch (SocketException s) {
            s.printStackTrace();
        }
        return null;
    }
}
