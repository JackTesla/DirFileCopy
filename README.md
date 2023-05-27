# DirFileCopy
This is java program which can copy one dir's files to different disk and one file only copy once.

A program that realizes the distribution of a folder to multiple disks.
Function:
1. You can freely set the refresh interval, the minimum is 1 second, and regularly check whether the files to be transferred have been updated in the specified folder;
2. You can freely set the directory to read the folder, and multiple directories to write to the disk;
3. When the disk currently has a writing task, or when the disk space is insufficient, the file is automatically transferred to the next disk, that is, each disk can only transfer one file at a time; when all disks are being written, stop the transfer , and then transfer when the disk is free;
4. A file can only be transferred to one disk;
5. The file name supports setting in the form of wildcards. Software under Windows
6. The files that have been transferred under the same folder last time will no longer be copied

Instructions:
1. Install jdk8 or above
2. The command line enters the directory of this java file
3. Execute javac DirFileCopy.java
4. Execute java DirFileCopy
5. As long as the location of the java file remains unchanged, you only need to enter the directory where the java file is located and execute java DirFileCopy

实现某一文件夹向多个磁盘分发的程序。

功能：

1.可以自由设置刷新间隔，最低1秒，定时检测指定的文件夹下是否更新了待传输的文件；

2.可以自由设置读取文件夹的目录，和多个写入磁盘的目录；

3.当磁盘当前正存在有写入任务时，或磁盘空间不足时，自动向下一个磁盘传输文件，即每个磁盘同时仅可传输一个文件；当所有磁盘都处于正在写入时，停止传输，待有磁盘空闲时再进行传输；

4.一个文件仅可被传输给一个磁盘；

5.文件名支持以通配符的形式来设置。Windows下的软件

6.上次同一文件夹下已传输完成的文件不再复制

使用方法：

1.安装jdk8以上

2.命令行进入此java文件的目录

3. 执行 javac DirFileCopy.java

4. 执行 java DirFileCopy

5. 只要此java文件位置没变，再次执行只需进入java文件所在目录执行 java DirFileCopy
