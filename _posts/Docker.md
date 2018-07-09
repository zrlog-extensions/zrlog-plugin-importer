title: Docker --- 安装
date: '2018-06-04 19:15:58'
updated: '2018-06-04 19:15:58'
tags: [Docker]
permalink: /articles/2018/06/04/1528110957634.html
---
```
安装Docker
  Docker支持在主流的操作系统平台上使用，包括Ubuntu·CentOS·Windows以及MacOS系统等，当然在Linux系列平台上是原生支持，使用体验是最好。
  
  Ubuntu
	1 Ubuntu 14.04及以上版本
	
	Ubuntu 14.04版本官方软件源中已经自带了Docker包，可以直接安装：
	--------------------------------------------------------------------
	$ sudo apt- get update 
	$ sudo apt- get install -y docker. io 
	$ sudo ln -sf /usr/ bin/ docker. io /usr/ local/ bin/ docker 
	$ sudo sed -i '$acomplete -F _docker docker' /etc/ bash_ completion. d/ docker. io 
	--------------------------------------------------------------------
	
	以上流程使用Ubuntu 14. 04 系统默认自带docker. io 安装包安装Docker，这样安装的Docker 版本相对较旧。
	读者也可通过下面的方法从Docker 官方源安装最新版本。
	首先需要安装apt- transport- https，并添加Docker 官方源：
	--------------------------------------------------------------------
	$ sudo apt- get install apt- transport- https
	$ sudo apt- key adv --keyserver hkp:// keyserver. ubuntu. com: 80 --recv- keys 36A1D7869245C8950F966E92D8576A8BA88D21E9 
	$ sudo bash -c "echo deb https:// get. docker. io/ ubuntu docker main > /etc/ apt/ sources. list. d/ docker. list" 
	$ sudo apt- get update 
	--------------------------------------------------------------------
	
	之后，可以通过下面的命令来安装最新版本的Docker：
	--------------------------------------------------------------------
	$ sudo apt-get install -y lxc-docker
	--------------------------------------------------------------------
	
	在安装了Docker官方软件源后，若需要更新Docker软件版本，只需要执行以下命令即可升级;
	--------------------------------------------------------------------
	$ sudo apt-get update -y lxc-docker
	--------------------------------------------------------------------
	后文中使用$作为终端引导符号，表示非root权限用户，#代表是root用户。
	
	2 Ubuntu14.04以下的版本
	 
	 如果使用的是较低版本的Ubuntu系统，则需要先进行内核更新并重启系统后再进行安装;
	 --------------------------------------------------------------------
	 $ sudo apt-get update
	 $ sudo apt-get install -y linux-image-generic-lts-raring linux-headers-generic-lts-raring
	 $ sudo reboot
	 --------------------------------------------------------------------
	 重启后，重复再Ubuntu14.04系统的安装步骤即可。
	 
  CentOS
	Docker支持CentOS 6及以后的版本。
	对于CentOS 6系统可使用EPEL库安装Docker，命令如下;
	--------------------------------------------------------------------
	$ sudo yun install -y http://mirrors.yun-idc.com/epel/6/i386/epel-release-6-8.noarch.rpm
	$ sudo yum install -y docker-io
	--------------------------------------------------------------------
	
	对于CentOS 7系统，有于CentOS-Extras源中已经内置Docker，读者可以直接使用yum源命令进行安装：
	--------------------------------------------------------------------
	$ yum install -y docker
	--------------------------------------------------------------------
	目前在CentOS系统中更新Docker软件有两种方法：一自行通过源码编译安装，二下载二进制文件进行更新。
	
  Windows
	目前Docker官方已经宣布Docker通过虚拟机方式支持Windows 7,8,10 , 前提是主机的CPU支持硬件虚拟化。由于近几年发布的Inter和AMD CPU基本上都已经支持硬件虚拟化特性，因此在Windows中使用Docker通常不会有硬件支持的问题。
	由于Docker引擎使用Linux内核特性，所以在Windows上运行的话，需要额外使用一个虚拟机来提供Linux支持。
	这里推荐使用Boot2Docker工具，它会首先安装一个经过加工与配置的VirtualBox轻量级虚拟机，然后在其中运行Docker。
	主要步骤如下： 
	  1）从https://docs.docker.com/installation/windows/下载最新官方Docker  for  Windows  Installer。
	  2）运行Installer。这个过程将安装VirtualBox，MSYS-git，boot2docker  Linux ISO镜像，以及Boot2Docker管理工具。
	  3）打开桌面的Boot2Docker  Start程序，或者用以下命令：Program Files>Boot2Docker  for  Windows。此初始化脚本在第一次运行时需要输入一个SSH  Key Passphrase（用于SSH密钥生成的口令）。读者可以自行设定，也可以直接按回车键，跳过此设定；此时BootDocker start程序将连接至虚拟机中的shell会话，Docker已经启动了。
	  
  Mac OS 
	目前Docker 已经支持Mac OS X 10. 6Snow Leopard 及以上版本的Mac OS。
	在Mac OS 上使用Docker，同样需要Boot2Docker 工具的支持。
	主要步骤如下：
	  1）下载最新官方Docker for OS X Installer。读者可以从https://docs.docker.com/installation/mac/ 下载。
	  2）双击运行安装包。这个过程将安装一个VirtualBox 虚拟机、Docker 本身以及Boot2Docker 管理工具
	  3）安装成功后，找到Boot2Docker（Mac 系统的Application 或“应用”文件夹中）并运行它。现在进行Boot2Docker 的初始化：
	  --------------------------------------------------------------------
	  $ boot2docker init
	  $ boot2docker start 
	  --------------------------------------------------------------------
	  读者将看到虚拟机在命令行窗口中启动运行，并显示Docker的启动信息，则说明Docker安装成功。当虚拟机初始化完毕后，可以使用boot2docker stop 和 boot2docker start 来控制。

```
