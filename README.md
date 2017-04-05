# AImage
## 一个非常简单的Android图片加载框架<br/>
使用DiskLruCache实现的三级缓存，代码量非常小。<br/>
你可能会奇怪，像glide,picasso,fresco,imageloader等等一大堆图片加载框架，为什么还要写一套。<br/>

因为有时候做一些中间件sdk开发，尽可能的减少对第三方开源库的依赖，而且这些框架每一个都上百KB的,输出的sdk包能小则小；所以整理了一下DiskLruCache的使用
