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

## 处理思路
使用自己定制的数据集训练Faster-RCNN模型，一般有两种思路：其一，修改Faster-RCNN代码,适合自己的数据集；其二，将自己的数据集格式改为VOC2007形式的数据集。从工作量上看，无疑后者更加容易一些（下面的例子采取第二种方法）。

## 数据处理
从最原始的Faster-RCNN来看，VOC2007格式的数据格式如下所示：
![](/images/voc2007-folder.png)

Annotations中存在的是.xml文件，文件中记录描述每张图的ground truth信息,如下所示：
![](/images/annotations-sample-xml.png)

##
## Trouble Shooting
### 'max_overlaps' issue
使用自己数据集训练Faster-RCNN模型时，如果出现'max_overlaps' issue， 极有可能是因为之前训练时出现错误，但pkl文件仍在cache中。所以解决的方法是删除在py-faster-rcnn/data/cache目录下的pkl文件。
