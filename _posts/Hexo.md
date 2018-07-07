---
title: Hello Hexo 
toc: true
tags:
  - Hexo
categories:
  - DevTools 
date: 2015-2-07 15:28:52
---

这是一篇Hexo的简单使用教程，使用前提是安装hexo并部署到GitPage，并且采用git方式部署文章。

<!--more-->
## 写作部分
`hexo new draft "title"`

**发布草稿**：在_draft文件夹中创建md文件

** 本质上就是利用模板draft来创建文章，模板种类在scaffolds文件夹中查看。**

`hexo publish "title"`
将草稿发布为文章：将_draft文件夹中已创建的文件移动到_post文件夹中，等待生成文章到public文件夹中。

## 本地调试部分：在本地查看效果

`hexo s`

启动本地服务器，访问[http://localhost:4000](http://localhost:4000)查看待发布文章。 
**Ctrl+C**结束关闭本地服务器。

`hexo g`


**root用户**
本地生成待部署的文章，待部署的文件会保存到public文件夹中。

## 部署部分：部署到GitPage

使用下面命令安装`git`,然后修改配置文件`_config.yml`.
` npm install hexo-deployer-git --save`
然后修改配置文件`_config.yml`.

```
deploy:
  type: git
  repository: git@github.com:Simshang/Simshang.github.io.git
  # branch: master
  #
```

最后使用`hexo d`进行部署

## 清除生成文件

`hexo clean`

** 如果在部署GitPage过程中有`CNAME`文件，记得放进`Public`文件夹**

> `CNAME`文件是将自己域名和GitPage关联的文件

### **重置GitPage**

1. 在GitHub重建`github.io仓库`

2. 在`hexo init`的文件夹下删除`.deploy_git`文件夹

3. 在终端`hexo d -g`, 重新将生成的文件进行部署