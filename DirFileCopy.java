import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.*;

/**
 * 实现某一文件夹向多个磁盘分发的程序。
 * 要求：
 * 1.可以自由设置刷新间隔，最低1秒，定时检测指定的文件夹下是否更新了待传输的文件；
 * 2.可以自由设置读取文件夹的目录，和多个写入磁盘的目录；
 * 3.当磁盘当前正存在有写入任务时，或磁盘空间不足时，自动向下一个磁盘传输文件，即每个磁盘同时仅可传输一个文件；当所有磁盘都处于正在写入时，停止传输，待有磁盘空闲时再进行传输；
 * 4.一个文件仅可被传输给一个磁盘；
 * 5.文件名支持以通配符的形式来设置。Windows下的软件
 */

public class DirFileCopy {

    private final int refreshInterval; // 刷新磁盘列表，默认1s
    private final int maxThreads;  // 最大线程数
    private final String srcDirPath;  // 源目录
    private final String srcDisk;
    private final String dstDir;  // 目的目录
    private final String finishedLogPath;
    private final ConcurrentLinkedDeque<String> dstDisks; // 目的磁盘
    private final ConcurrentLinkedQueue<String> dstDisksStack
            = new ConcurrentLinkedQueue<>(); // 空闲磁盘队列

    private final ConcurrentLinkedQueue<String> taskFileList = new ConcurrentLinkedQueue<>();
//    private final ConcurrentLinkedQueue<String> finishedFileList = new ConcurrentLinkedQueue<>();
    private final ConcurrentSkipListSet<String> allFileSet = new ConcurrentSkipListSet<>();
    private final ScheduledThreadPoolExecutor scheduledExec = new ScheduledThreadPoolExecutor(1); // 单线程更新
    private final ThreadPoolExecutor executor;

    public DirFileCopy() {

        File[] roots = File.listRoots();
        printDisksInfo(roots);
        Scanner scanner = new Scanner(System.in);
        System.out.println("请输入读取文件夹的完整路径：(例如: C://myplots)");
        this.srcDirPath = scanner.nextLine();

        // 设置 dstDisks
        ConcurrentLinkedDeque<String> tmpDstDisksList=new ConcurrentLinkedDeque<>();
        this.srcDisk=srcDirPath.split("//")[0]+"//";
        for(File root : roots) {
            if(root.toString().charAt(0) != srcDisk.charAt(0) && root.getUsableSpace()>0)
                tmpDstDisksList.add(root.toString());
        }
        System.out.println("\n将写入下列磁盘："+",空间不够的磁盘将自动跳过\n"+tmpDstDisksList);
        System.out.println("确认请输入Y, 修改请输入N:");
        String replyString=scanner.nextLine();
        if (replyString != "Y" && replyString != "y"
                && replyString.charAt(0)!= 'Y' && replyString.charAt(0)!= 'y') {
            dstDisks = new ConcurrentLinkedDeque<>();
            System.out.println("\n请输入写入的磁盘盘符，(区分大小写) 用空格隔开：(例如：D E F G):");
            String[] tmpDstDisks=scanner.nextLine().split(" ");
            for(String diska:tmpDstDisks) {
                if(diska.isEmpty()) continue;
                char c=diska.charAt(0);
                if((c>='a' && c<='z') || (c>='A' && c<='Z') && c!=srcDisk.charAt(0)){
                    File file=new File(diska);
                    if(file.getUsableSpace()>0) dstDisks.add(diska);
                }
            }
        } else dstDisks=tmpDstDisksList;
        for(String dstDisk:dstDisks) dstDisksStack.add(dstDisk);

        System.out.println("\n请输入写入的目录：(例如: myplots，就会在所有目的磁盘中的myplots下写入)");
        this.dstDir = scanner.nextLine();
        for(String dstDisk:dstDisks) new File(dstDisk+dstDir).mkdirs();
        System.out.println("\n请输入刷新间隔：(例如：1 代表1s）");
        this.refreshInterval=scanner.nextInt();
        System.out.println("\n请输入最大线程数（例如：100）:");
        this.maxThreads=scanner.nextInt();
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(maxThreads);

        Path path = Paths.get(srcDirPath,"finishedFiles.txt");
        finishedLogPath=path.toString();
        try {
            File finishedLog = new File(finishedLogPath);
            Scanner logScanner = new Scanner(finishedLog);
            while (logScanner.hasNextLine()) {
                allFileSet.add(logScanner.nextLine());
            }
        } catch (FileNotFoundException e) {
            ; // can't do anything
        }
        intermittentlyRefreshsrcDir();

    }

    private static double byteToGB(long bytes) {return bytes/1024.0/1024/1024;}

    public static void main(String[] args) throws InterruptedException {
        DirFileCopy fileAndDirCopy=new DirFileCopy();
        System.out.println(fileAndDirCopy);
        fileAndDirCopy.startCopy();

    }

    private void intermittentlyRefreshsrcDir() {
        System.out.println("源目录:"+this.srcDirPath);
        File srcDirFile = new File(srcDirPath);
        File[] allFiles=srcDirFile.listFiles();
//        System.out.println("allFileSet size:"+allFileSet.size());
        for (File file : allFiles) {
            if(file.isFile() && (allFileSet.add(file.toString())) && (!file.toString().equals(finishedLogPath))) {
                System.out.println(file);
                taskFileList.add(file.toString());
//                allFileSet.add(file.toString());
            }
        }
//        System.out.println("allFileSet size:"+allFileSet.size());
    }

    
    public void startCopy() {
        // 开启定时刷新任务
        scheduledExec.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                intermittentlyRefreshsrcDir();
            }
        }, 0,refreshInterval, TimeUnit.SECONDS);

        int threadId=0;
        while (!dstDisksStack.isEmpty()) {
            int finalThreadId = threadId;
            System.out.println(threadId++);
            String dstDisk=dstDisksStack.poll();
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    copyWorker(finalThreadId,dstDisk);
                }
            });
        }
    }

    private void writeStringToFile(String filename,String str) {
        FileWriter fw = null;
        BufferedWriter bw = null;
        PrintWriter pw = null;
        try {
            fw = new FileWriter(filename, true);
            bw = new BufferedWriter(fw);
            pw = new PrintWriter(bw);

            pw.println(str);
            pw.flush();
        }catch (Exception e) {
            e.printStackTrace();
            System.out.println("\nerror: 写入已完成的文件到 "+filename+" 失败!");
        } finally {
            try {
                pw.close();
                bw.close();
                fw.close();
            } catch (IOException io) {// can't do anything }
            }
        }
    }

    private void copyWorker(int id, String dstDisk) {
        while(true) { // 无限循环，等待任务
            try {
                String taskFile;
                if ((taskFile = taskFileList.poll()) != null) {
                    File srcFile = new File(taskFile);
                    File dstFile = new File(dstDisk+dstDir,srcFile.getName());
                    if(dstFile.getParentFile().getUsableSpace()<srcFile.length()) {
                        System.out.println("空闲空间: "+dstFile.getParentFile().getFreeSpace()
                                +" 源文件大小："+srcFile.length());
                        System.out.println("磁盘"+dstDisk+"空间不足，线程"+id+"退出！");
                        break;
                    }
                    System.out.println("id="+id + " " + dstFile);
                    copyByBufferedInOutStream(srcFile,dstFile);
                    writeStringToFile(finishedLogPath,taskFile);
                } else {
                    System.out.println(dstDisk+" id="+id+"无任务，休眠");
                    TimeUnit.SECONDS.sleep(5);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    public static void copyByBufferedInOutStream(File scrFile,File destFile) throws IOException {
        byte[] bytes=new byte[8*1024];
        try (InputStream in = new BufferedInputStream(new FileInputStream(scrFile))) {
            OutputStream out = new BufferedOutputStream(new FileOutputStream(destFile));
            int count;
            while ((count = in.read(bytes)) > 0) {
                out.write(bytes,0,count);
            }
        }
    }

    private void printDisksInfo(File[] roots) {
        System.out.println("所有磁盘信息如下：");
        for (File root : roots) {
            root.toString();
            System.out.print(root);
            long usable=root.getUsableSpace();
            long total=root.getTotalSpace();
            long used = total-usable;
            System.out.printf("  总空间: %.2fGB ",byteToGB(usable));
            System.out.printf("  可用空间: %.2fGB ",byteToGB(usable));
            System.out.printf("  已用空间: %.2fGB\n",byteToGB(used));
        }
    }

    @Override
    public String toString() {
        return "配置如下{\n" +
                " 刷新间隔=" + refreshInterval +
                "\n 源目录='" + srcDirPath + '\'' +
                "\n 写入目录='" + dstDir + '\'' +
                "\n 写入磁盘=" + dstDisks +
                '}';
    }
}
