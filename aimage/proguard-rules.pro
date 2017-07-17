-optimizationpasses 5
-dontusemixedcaseclassnames
#不混淆输入的类文件
#-dontobfuscate
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-ignorewarnings
#声明不压缩输入文件。
-dontshrink
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
-keepattributes Signature

-keep class com.jzy.aimage.BitmapHelper{
    public *;
}
