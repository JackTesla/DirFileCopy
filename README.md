# DirFileCopy
This is java program which can copy one dir's files to different disk and one file only copy once.

实现某一文件夹向多个磁盘分发的程序。
功能：
1.可以自由设置刷新间隔，最低1秒，定时检测指定的文件夹下是否更新了待传输的文件；

2.可以自由设置读取文件夹的目录，和多个写入磁盘的目录；

3.当磁盘当前正存在有写入任务时，或磁盘空间不足时，自动向下一个磁盘传输文件，即每个磁盘同时仅可传输一个文件；当所有磁盘都处于正在写入时，停止传输，待有磁盘空闲时再进行传输；

4.一个文件仅可被传输给一个磁盘；

5.文件名支持以通配符的形式来设置。Windows下的软件
