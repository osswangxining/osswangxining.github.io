---
title: 使用定制数据集训练Faster-RCNN模型
date: 2017-10-10 20:46:25
categories:
  - Machine Learning
  - Deep Learning
tags:
  - AI
  - CNN
  - RCNN
  - DL
  - ML

---

## 基本概念介绍

目标检测的四个基本步骤：
- 候选区域生成
- 特征提取
- 分类
- 位置精修

RCNN的算法：
- 1. 将一张图片生成2K个候选区域；
- 2. 对每个候选区域，使用深度网络(deep net)提取特征；
- 3. 特征送入每一类的SVM分类器，判别是否属于该类；
- 4. 使用回归器精细修正候选框的位置；

从RCNN到fast RCNN，再到faster RCNN，目标检测的四个基本步骤终于被统一到一个深度网络框架之内。所有计算没有重复，完全在GPU中完成，大大提高了运行速度。
![](/images/rcnn-fast-faster.png)

fast RCNN在RCNN的基础之上，将分类和位置精修统一到了一个深度网络之内。faster RCNN可以简单地看做“区域生成网络+fast RCNN“的系统，用区域生成网络代替fast RCNN中的Selective Search方法。

## Faster-RCNN的配置与编译
### 配置Caffe环境
下载有关Caffe的所有的依赖项，可以查看安装Caffe的教程
### 配置Faster-RCNN环境
安装cython,python-opencv,easydict:
```
sudo apt-get install python-numpy
sudo apt-get install python-scipy
pip install cython
pip install easydict
pip install shutil
pip install cPickle
pip install uuid
pip install multiprocessing
pip install xml
```
  下载py-faster-rcnn
```
# Make sure to clone with --recursive  
git clone --recursive https://github.com/rbgirshick/py-faster-rcnn.git
```

  进入py-faster-rcnn/lib，执行
```
make
```

  进入py-faster-rcnn/caffe-faster-rcnn，将我改好的Makefile.config复制该路径下:

## 处理思路
使用自己定制的数据集训练Faster-RCNN模型，一般有两种思路：其一，修改Faster-RCNN代码,适合自己的数据集；其二，将自己的数据集格式改为VOC2007形式的数据集。从工作量上看，无疑后者更加容易一些（下面的例子采取第二种方法）。

## 数据处理
从最原始的Faster-RCNN来看，VOC2007格式的数据格式如下所示：
![](/images/voc2007-folder.png)

Annotations中存在的是.xml文件，文件中记录描述每张图的ground truth信息,如下所示：
![](/images/annotations-sample-xml.png)

每个xml文件的内容具体如下：
```
<annotation>
	<folder>shoe</folder>
	<filename>shoe-001.jpg</filename>
	<path>/home/xiningwang/Downloads/shoe/shoe-001.jpg</path>
	<source>
		<database>Unknown</database>
	</source>
	<size>
		<width>1000</width>
		<height>1000</height>
		<depth>3</depth>
	</size>
	<segmented>0</segmented>
	<object>
		<name>shoe</name>
		<pose>Unspecified</pose>
		<truncated>0</truncated>
		<difficult>0</difficult>
		<bndbox>
			<xmin>44</xmin>
			<ymin>150</ymin>
			<xmax>975</xmax>
			<ymax>842</ymax>
		</bndbox>
	</object>
</annotation>
```

ImageSets/Main存放的是test.txt、trainval.txt、train.txt、val.txt。 其中，test.txt是测试集，大概占整个数据集的20%；trainval.txt是训练集和验证集的组合，也就是整个数据集剩下的80%；train.txt是训练集，是trainval.txt的90%；val.txt是验证集，是trainval.txt的10%。

每个TXT文件的内容都是相应图片的前缀（去掉后缀.jpg），如下所示：
```
handbag-001
handbag-002
handbag-003
handbag-004
handbag-005
handbag-006
handbag-007
handbag-008
handbag-009
handbag-010
handbag-011
handbag-012
shoe-001
shoe-002
shoe-003
shoe-004
shoe-005
shoe-006
shoe-007
shoe-008
shoe-009
shoe-010
shoe-011
shoe-012
```

JPEGImages中存放的是.jpg图片，如下所示：
![](/images/jpegimages-sample.png)


##
## Trouble Shooting
### 'max_overlaps' issue
使用自己数据集训练Faster-RCNN模型时，如果出现'max_overlaps' issue， 极有可能是因为之前训练时出现错误，但pkl文件仍在cache中。所以解决的方法是删除在py-faster-rcnn/data/cache目录下的pkl文件。
