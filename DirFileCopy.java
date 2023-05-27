import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.*;

/**
 * ʵ��ĳһ�ļ����������̷ַ��ĳ���
 * Ҫ��
 * 1.������������ˢ�¼�������1�룬��ʱ���ָ�����ļ������Ƿ�����˴�������ļ���
 * 2.�����������ö�ȡ�ļ��е�Ŀ¼���Ͷ��д����̵�Ŀ¼��
 * 3.�����̵�ǰ��������д������ʱ������̿ռ䲻��ʱ���Զ�����һ�����̴����ļ�����ÿ������ͬʱ���ɴ���һ���ļ��������д��̶���������д��ʱ��ֹͣ���䣬���д��̿���ʱ�ٽ��д��䣻
 * 4.һ���ļ����ɱ������һ�����̣�
 * 5.�ļ���֧����ͨ�������ʽ�����á�Windows�µ����
 */

public class DirFileCopy {

    private final int refreshInterval; // ˢ�´����б�Ĭ��1s
    private final int maxThreads;  // ����߳���
    private final String srcDirPath;  // ԴĿ¼
    private final String srcDisk;
    private final String dstDir;  // Ŀ��Ŀ¼
    private final String finishedLogPath;
    private final ConcurrentLinkedDeque<String> dstDisks; // Ŀ�Ĵ���
    private final ConcurrentLinkedQueue<String> dstDisksStack
            = new ConcurrentLinkedQueue<>(); // ���д��̶���

    private final ConcurrentLinkedQueue<String> taskFileList = new ConcurrentLinkedQueue<>();
//    private final ConcurrentLinkedQueue<String> finishedFileList = new ConcurrentLinkedQueue<>();
    private final ConcurrentSkipListSet<String> allFileSet = new ConcurrentSkipListSet<>();
    private final ScheduledThreadPoolExecutor scheduledExec = new ScheduledThreadPoolExecutor(1); // ���̸߳���
    private final ThreadPoolExecutor executor;

    public DirFileCopy() {

        File[] roots = File.listRoots();
        printDisksInfo(roots);
        Scanner scanner = new Scanner(System.in);
        System.out.println("�������ȡ�ļ��е�����·����(����: C://myplots)");
        this.srcDirPath = scanner.nextLine();

        // ���� dstDisks
        ConcurrentLinkedDeque<String> tmpDstDisksList=new ConcurrentLinkedDeque<>();
        this.srcDisk=srcDirPath.split("//")[0]+"//";
        for(File root : roots) {
            if(root.toString().charAt(0) != srcDisk.charAt(0) && root.getUsableSpace()>0)
                tmpDstDisksList.add(root.toString());
        }
        System.out.println("\n��д�����д��̣�"+",�ռ䲻���Ĵ��̽��Զ�����\n"+tmpDstDisksList);
        System.out.println("ȷ��������Y, �޸�������N:");
        String replyString=scanner.nextLine();
        if (replyString != "Y" && replyString != "y"
                && replyString.charAt(0)!= 'Y' && replyString.charAt(0)!= 'y') {
            dstDisks = new ConcurrentLinkedDeque<>();
            System.out.println("\n������д��Ĵ����̷���(���ִ�Сд) �ÿո������(���磺D E F G):");
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

        System.out.println("\n������д���Ŀ¼��(����: myplots���ͻ�������Ŀ�Ĵ����е�myplots��д��)");
        this.dstDir = scanner.nextLine();
        for(String dstDisk:dstDisks) new File(dstDisk+dstDir).mkdirs();
        System.out.println("\n������ˢ�¼����(���磺1 ����1s��");
        this.refreshInterval=scanner.nextInt();
        System.out.println("\n����������߳��������磺100��:");
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
        System.out.println("ԴĿ¼:"+this.srcDirPath);
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
        // ������ʱˢ������
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
            System.out.println("\nerror: д������ɵ��ļ��� "+filename+" ʧ��!");
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
        while(true) { // ����ѭ�����ȴ�����
            try {
                String taskFile;
                if ((taskFile = taskFileList.poll()) != null) {
                    File srcFile = new File(taskFile);
                    File dstFile = new File(dstDisk+dstDir,srcFile.getName());
                    if(dstFile.getParentFile().getUsableSpace()<srcFile.length()) {
                        System.out.println("���пռ�: "+dstFile.getParentFile().getFreeSpace()
                                +" Դ�ļ���С��"+srcFile.length());
                        System.out.println("����"+dstDisk+"�ռ䲻�㣬�߳�"+id+"�˳���");
                        break;
                    }
                    System.out.println("id="+id + " " + dstFile);
                    copyByBufferedInOutStream(srcFile,dstFile);
                    writeStringToFile(finishedLogPath,taskFile);
                } else {
                    System.out.println(dstDisk+" id="+id+"����������");
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
        System.out.println("���д�����Ϣ���£�");
        for (File root : roots) {
            root.toString();
            System.out.print(root);
            long usable=root.getUsableSpace();
            long total=root.getTotalSpace();
            long used = total-usable;
            System.out.printf("  �ܿռ�: %.2fGB ",byteToGB(usable));
            System.out.printf("  ���ÿռ�: %.2fGB ",byteToGB(usable));
            System.out.printf("  ���ÿռ�: %.2fGB\n",byteToGB(used));
        }
    }

    @Override
    public String toString() {
        return "��������{\n" +
                " ˢ�¼��=" + refreshInterval +
                "\n ԴĿ¼='" + srcDirPath + '\'' +
                "\n д��Ŀ¼='" + dstDir + '\'' +
                "\n д�����=" + dstDisks +
                '}';
    }
}
