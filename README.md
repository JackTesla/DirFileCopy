# DirFileCopy
This is java program which can copy one dir's files to different disk and one file only copy once.

A program that realizes the distribution of a folder to multiple disks.
Function:
1. You can freely set the refresh interval, the minimum is 1 second, and regularly check whether the files to be transferred have been updated in the specified folder;
2. You can freely set the directory to read the folder, and multiple directories to write to the disk;
3. When the disk currently has a writing task, or when the disk space is insufficient, the file is automatically transferred to the next disk, that is, each disk can only transfer one file at a time; when all disks are being written, stop the transfer , and then transfer when the disk is free;
4. A file can only be transferred to one disk;
5. The file name supports setting in the form of wildcards. Software under Windows

实现某一文件夹向多个磁盘分发的程序。

功能：

1.可以自由设置刷新间隔，最低1秒，定时检测指定的文件夹下是否更新了待传输的文件；

2.可以自由设置读取文件夹的目录，和多个写入磁盘的目录；

3.当磁盘当前正存在有写入任务时，或磁盘空间不足时，自动向下一个磁盘传输文件，即每个磁盘同时仅可传输一个文件；当所有磁盘都处于正在写入时，停止传输，待有磁盘空闲时再进行传输；

4.一个文件仅可被传输给一个磁盘；

5.文件名支持以通配符的形式来设置。Windows下的软件
