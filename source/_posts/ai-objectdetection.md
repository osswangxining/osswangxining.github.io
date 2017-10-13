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
```
## Refer to http://caffe.berkeleyvision.org/installation.html
# Contributions simplifying and improving our build system are welcome!

# cuDNN acceleration switch (uncomment to build with cuDNN).
USE_CUDNN := 1

# CPU-only switch (uncomment to build without GPU support).
# CPU_ONLY := 1

# uncomment to disable IO dependencies and corresponding data layers
# USE_OPENCV := 0
# USE_LEVELDB := 0
# USE_LMDB := 0

# uncomment to allow MDB_NOLOCK when reading LMDB files (only if necessary)
#	You should not set this flag if you will be reading LMDBs with any
#	possibility of simultaneous read and write
# ALLOW_LMDB_NOLOCK := 1

# Uncomment if you're using OpenCV 3
OPENCV_VERSION := 3

# To customize your choice of compiler, uncomment and set the following.
# N.B. the default for Linux is g++ and the default for OSX is clang++
# CUSTOM_CXX := g++

# CUDA directory contains bin/ and lib/ directories that we need.
CUDA_DIR := /usr/local/cuda
# On Ubuntu 14.04, if cuda tools are installed via
# "sudo apt-get install nvidia-cuda-toolkit" then use this instead:
# CUDA_DIR := /usr

# CUDA architecture setting: going with all of them.
# For CUDA < 6.0, comment the *_50 through *_61 lines for compatibility.
# For CUDA < 8.0, comment the *_60 and *_61 lines for compatibility.
CUDA_ARCH := -gencode arch=compute_20,code=sm_20 \
		-gencode arch=compute_20,code=sm_21 \
		-gencode arch=compute_30,code=sm_30 \
		-gencode arch=compute_35,code=sm_35 \
		-gencode arch=compute_50,code=sm_50 \
		-gencode arch=compute_52,code=sm_52 \
		-gencode arch=compute_60,code=sm_60 \
		-gencode arch=compute_61,code=sm_61 \
		-gencode arch=compute_61,code=compute_61

# BLAS choice:
# atlas for ATLAS (default)
# mkl for MKL
# open for OpenBlas
BLAS := open
# Custom (MKL/ATLAS/OpenBLAS) include and lib directories.
# Leave commented to accept the defaults for your choice of BLAS
# (which should work)!
# BLAS_INCLUDE := /path/to/your/blas
# BLAS_LIB := /path/to/your/blas

# Homebrew puts openblas in a directory that is not on the standard search path
# BLAS_INCLUDE := $(shell brew --prefix openblas)/include
# BLAS_LIB := $(shell brew --prefix openblas)/lib

# This is required only if you will compile the matlab interface.
# MATLAB directory should contain the mex binary in /bin.
# MATLAB_DIR := /usr/local
# MATLAB_DIR := /Applications/MATLAB_R2012b.app

# NOTE: this is required only if you will compile the python interface.
# We need to be able to find Python.h and numpy/arrayobject.h.
PYTHON_INCLUDE := /usr/include/python2.7 \
		/usr/lib/python2.7/dist-packages/numpy/core/include
# Anaconda Python distribution is quite popular. Include path:
# Verify anaconda location, sometimes it's in root.
# ANACONDA_HOME := $(HOME)/anaconda
# PYTHON_INCLUDE := $(ANACONDA_HOME)/include \
		# $(ANACONDA_HOME)/include/python2.7 \
		# $(ANACONDA_HOME)/lib/python2.7/site-packages/numpy/core/include

# Uncomment to use Python 3 (default is Python 2)
# PYTHON_LIBRARIES := boost_python3 python3.5m
# PYTHON_INCLUDE := /usr/include/python3.5m \
#                 /usr/lib/python3.5/dist-packages/numpy/core/include

# We need to be able to find libpythonX.X.so or .dylib.
PYTHON_LIB := /usr/lib
# PYTHON_LIB := $(ANACONDA_HOME)/lib

# Homebrew installs numpy in a non standard path (keg only)
# PYTHON_INCLUDE += $(dir $(shell python -c 'import numpy.core; print(numpy.core.__file__)'))/include
# PYTHON_LIB += $(shell brew --prefix numpy)/lib

# Uncomment to support layers written in Python (will link against Python libs)
 WITH_PYTHON_LAYER := 1

# Whatever else you find you need goes here.
INCLUDE_DIRS := $(PYTHON_INCLUDE) /usr/local/include /usr/include
LIBRARY_DIRS := $(PYTHON_LIB) /usr/local/lib /usr/lib
JAVA_HOME := /usr/lib/jvm/java-8-openjdk-ppc64el
INCLUDE_DIRS += $(JAVA_HOME)/include $(JAVA_HOME)/include/linux
INCLUDE_DIRS += /usr/include/hdf5/serial
LIBRARY_DIRS += /usr/lib/powerpc64le-linux-gnu/hdf5/serial
INCLUDE_DIRS += /opt/DL/nccl/include
LIBRARY_DIRS += /opt/DL/nccl/lib
INCLUDE_DIRS += /usr/local/lib/python2.7/dist-packages/numpy/core/include
LIBRARY_DIRS += /usr/local/nvidia/lib /usr/local/nvidia/lib64  /opt/DL/nccl/lib /opt/DL/openblas/lib

# If Homebrew is installed at a non standard location (for example your home directory) and you use it for general dependencies
# INCLUDE_DIRS += $(shell brew --prefix)/include
# LIBRARY_DIRS += $(shell brew --prefix)/lib

# NCCL acceleration switch (uncomment to build with NCCL)
# https://github.com/NVIDIA/nccl (last tested version: v1.2.3-1+cuda8.0)
# USE_NCCL := 1

# Uncomment to use `pkg-config` to specify OpenCV library paths.
# (Usually not necessary -- OpenCV libraries are normally installed in one of the above $LIBRARY_DIRS.)
# USE_PKG_CONFIG := 1

# N.B. both build and distribute dirs are cleared on `make clean`
BUILD_DIR := build
DISTRIBUTE_DIR := distribute

# Uncomment for debugging. Does not work on OSX due to https://github.com/BVLC/caffe/issues/171
# DEBUG := 1

# The ID of the GPU that 'make runtest' will use to run unit tests.
TEST_GPUID := 0

# enable pretty build (comment to see full commands)
Q ?= @

```

配置好Makefile.config文件，执行：
```
    make -j8 && make pycaffe
```

编译caffe时报错memcpy在此作用域中尚未声明,如下：
![](/images/frcnn-caffe-compile-error-gcc.png)

这可能是GCC的版本太高了，我们需要在makefile修改NVCCFLAGS变量，来强制将GCC的版本降低:
![](/images/frcnn-caffe-compile-error-gcc-version.png)

```
    NVCCFLAGS += -D_FORCE_INLINES -ccbin=$(CXX) -Xcompiler -fPIC $(COMMON_FLAGS)
```

## 处理思路
使用自己定制的数据集训练Faster-RCNN模型，一般有两种思路：其一，修改Faster-RCNN代码,适合自己的数据集；其二，将自己的数据集格式改为VOC2007形式的数据集。从工作量上看，无疑后者更加容易一些（下面的例子采取第二种方法）。

1. 图片的命名格式
虽然图片的命名理论上不会影响训练。因为训练的数据都是从txt文件中读取图片的名称。但是为了统一数据集，仍然建议批量、有规律的命名数据图片。
2. VOC格式的数据集格式
VOC格式的数据集格式如下：
```
---VOC2007
------Annotations
------ImagesSet
---------Main
------JPEGImages
```
3. Main中的四个txt文件
txt内容即是图片名字，所以遍历一遍JPEGImages或者Annotations都行.
```
test.txt是测试集，大概是整个数据集的50%；
trainval是训练和验证数据集，也就是整个数据集的剩余的50%
train.txt是训练集，是trainval的50%
val.txt是验证集，trainval剩余的50%
```


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

到此为止，你可以用自己定制的数据集的Annotations,ImageSets和JPEGImages替换py-faster-rcnn/data/VOCdevkit2007/VOC2007中对应的文件夹。

## 修改Faster-RCNN的训练参数
### 修改stage1_fast_rcnn_train.pt
第1步：py-faster-rcnn/models/pascal_voc/ZF/faster_rcnn_alt_opt/stage1_fast_rcnn_train.pt修改如下：
```
layer {  
  name: 'data'  
  type: 'Python'  
  top: 'data'  
  top: 'rois'  
  top: 'labels'  
  top: 'bbox_targets'  
  top: 'bbox_inside_weights'  
  top: 'bbox_outside_weights'  
  python_param {  
    module: 'roi_data_layer.layer'  
    layer: 'RoIDataLayer'  
    param_str: "'num_classes': 3" #按训练集类别改，该值为类别数+1  
  }  
}  
```

```
layer {  
  name: "cls_score"  
  type: "InnerProduct"  
  bottom: "fc7"  
  top: "cls_score"  
  param { lr_mult: 1.0 }  
  param { lr_mult: 2.0 }  
  inner_product_param {  
    num_output: 3 #按训练集类别改，该值为类别数+1  
    weight_filler {  
      type: "gaussian"  
      std: 0.01  
    }  
    bias_filler {  
      type: "constant"  
      value: 0  
    }  
  }  
}
```

```
layer {  
  name: "bbox_pred"  
  type: "InnerProduct"  
  bottom: "fc7"  
  top: "bbox_pred"  
  param { lr_mult: 1.0 }  
  param { lr_mult: 2.0 }  
  inner_product_param {  
    num_output: 12 #按训练集类别改，该值为（类别数+1）*4  
    weight_filler {  
      type: "gaussian"  
      std: 0.001  
    }  
    bias_filler {  
      type: "constant"  
      value: 0  
    }  
  }  
}
```

### 修改stage1_rpn_train.pt
第2步：py-faster-rcnn/models/pascal_voc/ZF/faster_rcnn_alt_opt/stage1_rpn_train.pt修改如下：
```
layer {  
    name: 'input-data'  
    type: 'Python'  
    top: 'data'  
    top: 'im_info'  
    top: 'gt_boxes'  
    python_param {  
        module: 'roi_data_layer.layer'  
        layer: 'RoIDataLayer'  
        param_str: "'num_classes': 3" #按训练集类别改，该值为类别数+1  
    }  
}  
```

### 修改stage2_fast_rcnn_train.pt
第3步：py-faster-rcnn/models/pascal_voc/ZF/faster_rcnn_alt_opt/stage2_fast_rcnn_train.pt修改如下：
```
layer {  
  name: 'data'  
  type: 'Python'  
  top: 'data'  
  top: 'rois'  
  top: 'labels'  
  top: 'bbox_targets'  
  top: 'bbox_inside_weights'  
  top: 'bbox_outside_weights'  
  python_param {  
    module: 'roi_data_layer.layer'  
    layer: 'RoIDataLayer'  
    param_str: "'num_classes': 3" #按训练集类别改，该值为类别数+1  
  }  
}
```

```
layer {  
  name: "cls_score"  
  type: "InnerProduct"  
  bottom: "fc7"  
  top: "cls_score"  
  param { lr_mult: 1.0 }  
  param { lr_mult: 2.0 }  
  inner_product_param {  
    num_output: 3 #按训练集类别改，该值为类别数+1  
    weight_filler {  
      type: "gaussian"  
      std: 0.01  
    }  
    bias_filler {  
      type: "constant"  
      value: 0  
    }  
  }  
}  
```

```
layer {  
  name: "bbox_pred"  
  type: "InnerProduct"  
  bottom: "fc7"  
  top: "bbox_pred"  
  param { lr_mult: 1.0 }  
  param { lr_mult: 2.0 }  
  inner_product_param {  
    num_output: 12 #按训练集类别改，该值为（类别数+1）*4  
    weight_filler {  
      type: "gaussian"  
      std: 0.001  
    }  
    bias_filler {  
      type: "constant"  
      value: 0  
    }  
  }  
}
```

### 修改stage2_rpn_train.pt
第4步：py-faster-rcnn/models/pascal_voc/ZF/faster_rcnn_alt_opt/stage2_rpn_train.pt修改:
```
layer {  
  name: 'input-data'  
  type: 'Python'  
  top: 'data'  
  top: 'im_info'  
  top: 'gt_boxes'  
  python_param {  
    module: 'roi_data_layer.layer'  
    layer: 'RoIDataLayer'  
    param_str: "'num_classes': 3" #按训练集类别改，该值为类别数+1  
  }  
}  
```

### 修改faster_rcnn_test.pt
第5步：py-faster-rcnn/models/pascal_voc/ZF/faster_rcnn_alt_opt/faster_rcnn_test.pt修改:
```
layer {  
  name: "cls_score"  
  type: "InnerProduct"  
  bottom: "fc7"  
  top: "cls_score"  
  inner_product_param {  
    num_output: 3 #按训练集类别改，该值为类别数+1  
  }  
}  
```

```
layer {  
  name: "bbox_pred"  
  type: "InnerProduct"  
  bottom: "fc7"  
  top: "bbox_pred"  
  inner_product_param {  
    num_output: 12 #按训练集类别改，该值为（类别数+1）*4  
  }  
}  
```

### 修改pascal_voc.py
  第6步：py-faster-rcnn/lib/datasets/pascal_voc.py修改:
```
class pascal_voc(imdb):  
def __init__(self, image_set, year, devkit_path=None):  
    imdb.__init__(self, 'voc_' + year + '_' + image_set)  
    self._year = year  
    self._image_set = image_set  
    self._devkit_path = self._get_default_path() if devkit_path is None \  
                        else devkit_path  
    self._data_path = os.path.join(self._devkit_path, 'VOC' + self._year)  
    self._classes = ('__background__', # always index 0  
                     '你的标签1','你的标签2')  
```

### 修改pascal_voc.py
第7步：修改lib/datasets/pascal_voc.py的_load_pascal_annotation()函数:
```
def _load_pascal_annotation(self, index):
    """
    Load image and bounding boxes info from XML file in the PASCAL VOC
    format.
    """
    filename = os.path.join(self._data_path, 'Annotations', index + '.xml')
    tree = ET.parse(filename)
    objs = tree.findall('object')
    if not self.config['use_diff']:
        # Exclude the samples labeled as difficult
        non_diff_objs = [
            obj for obj in objs if int(obj.find('difficult').text) == 0]
        # if len(non_diff_objs) != len(objs):
        #     print 'Removed {} difficult objects'.format(
        #         len(objs) - len(non_diff_objs))
        objs = non_diff_objs
    num_objs = len(objs)
    boxes = np.zeros((num_objs, 4), dtype=np.uint16)
    gt_classes = np.zeros((num_objs), dtype=np.int32)
    overlaps = np.zeros((num_objs, self.num_classes), dtype=np.float32)
    # "Seg" area for pascal is just the box area
    seg_areas = np.zeros((num_objs), dtype=np.float32)
    # Load object bounding boxes into a data frame.
    for ix, obj in enumerate(objs):
        bbox = obj.find('bndbox')
        # Make pixel indexes 0-based
        # x1 = float(bbox.find('xmin').text) - 1
        # y1 = float(bbox.find('ymin').text) - 1
        # x2 = float(bbox.find('xmax').text) - 1
        # y2 = float(bbox.find('ymax').text) - 1
        ### 因为标注过程中，可能存在标注的目标框太靠近边缘或者自己没注意，导致标注的数据在训练时产生了异常
        x1 = float(bbox.find('xmin').text)
        y1 = float(bbox.find('ymin').text)
        x2 = float(bbox.find('xmax').text)
        y2 = float(bbox.find('ymax').text)
        cls = self._class_to_ind[obj.find('name').text.lower().strip()]
        boxes[ix, :] = [x1, y1, x2, y2]
        gt_classes[ix] = cls
        overlaps[ix, cls] = 1.0
        seg_areas[ix] = (x2 - x1 + 1) * (y2 - y1 + 1)
    overlaps = scipy.sparse.csr_matrix(overlaps)
    return {'boxes' : boxes,
            'gt_classes': gt_classes,
            'gt_overlaps' : overlaps,
            'flipped' : False,
            'seg_areas' : seg_areas}
```

### 修改学习率
第8步：学习率可以在py-faster-rcnn/models/pascal_voc/ZF/faster_rcnn_alt_opt中的solve文件设置；

### 修改迭代次数
第9步：迭代次数可以在py-faster-rcnn/tools的train_faster_rcnn_alt_opt.py中修改。
```
    max_iters = [80000, 40000, 80000, 40000]
```
分别为4个阶段（rpn第1阶段，fast-rcnn第1阶段，rpn第2阶段，fast-rcnn第2阶段）的迭代次数。可改成你希望的迭代次数。
如果改了这些数值，最好把py-faster-rcnn/models/pascal_voc/ZF/faster_rcnn_alt_opt里对应的solver文件（有4个）也修改，stepsize小于上面修改的数值。

## 训练模型
我们采用ZF模型进行训练，输入如下命令：
```
    ./experiments/scripts/faster_rcnn_alt_opt.sh 0 ZF pascal_voc  
```

**！！！为防止与之前的模型搞混,训练前把output文件夹删除（或改个其他名），还要把py-faster-rcnn/data/cache中的文件和py-faster-rcnn/data/VOCdevkit2007/annotations_cache中的文件删除（如果有的话）**

## 测试模型

将训练得到的**py-faster-rcnn/output/faster-rcnn-alt-opt/voc_2007_trainval/ZF_faster_rcnn_final.caffemodel**模型拷贝到**py-faster-rcnn/data/faster_rcnn_model**

修改py-faster-rcnn/tools/demo.py或者新建demo-wangxn.py，具体代码如下所示：
```
#!/usr/bin/env python

# --------------------------------------------------------
# Faster R-CNN
# Copyright (c) 2015 Microsoft
# Licensed under The MIT License [see LICENSE for details]
# Written by Ross Girshick
# --------------------------------------------------------

"""
Demo script showing detections in sample images.

See README.md for installation instructions before running.
"""

import _init_paths
from fast_rcnn.config import cfg
from fast_rcnn.test import im_detect
from fast_rcnn.nms_wrapper import nms
from utils.timer import Timer
import matplotlib.pyplot as plt
import matplotlib
matplotlib.use('Agg')
import numpy as np
import scipy.io as sio
import caffe, os, sys, cv2
import argparse
import shutil

CLASSES = ('__background__',
           'handbag', 'shoe')

NETS = {'vgg16': ('VGG16',
                  'VGG16_faster_rcnn_final.caffemodel'),
        'zf': ('ZF',
                  'ZF_faster_rcnn_final_wangxn.caffemodel')}


def vis_detections(im, class_name, dets, image_name, thresh=0.5):
    """Draw detected bounding boxes."""
    inds = np.where(dets[:, -1] >= thresh)[0]
    if len(inds) == 0:
        return

    im = im[:, :, (2, 1, 0)]
    fig, ax = plt.subplots(figsize=(12, 12))
    ax.imshow(im, aspect='equal')
    for i in inds:
        bbox = dets[i, :4]
        score = dets[i, -1]

        ax.add_patch(
            plt.Rectangle((bbox[0], bbox[1]),
                          bbox[2] - bbox[0],
                          bbox[3] - bbox[1], fill=False,
                          edgecolor='red', linewidth=3.5)
            )
        ax.text(bbox[0], bbox[1] - 2,
                '{:s} {:.3f}'.format(class_name, score),
                bbox=dict(facecolor='blue', alpha=0.5),
                fontsize=14, color='white')

    ax.set_title(('{} detections with '
                  'p({} | box) >= {:.1f}').format(class_name, class_name,
                                                  thresh),
                  fontsize=14)
    plt.axis('off')
    plt.tight_layout()
    plt.draw()
    im_file = os.path.join('/home/wangxn', 'test100', 'new-{}'.format(image_name))
    plt.savefig(im_file)

def demo(net, image_name):
    """Detect object classes in an image using pre-computed object proposals."""

    # Load the demo image
    im_file = os.path.join(cfg.DATA_DIR, 'demo', image_name)
    im = cv2.imread(im_file)

    # Detect all object classes and regress object bounds
    timer = Timer()
    timer.tic()
    scores, boxes = im_detect(net, im)
    timer.toc()
    print ('Detection took {:.3f}s for '
           '{:d} object proposals').format(timer.total_time, boxes.shape[0])

    # Visualize detections for each class
    CONF_THRESH = 0.8
    NMS_THRESH = 0.3
    for cls_ind, cls in enumerate(CLASSES[1:]):
        cls_ind += 1 # because we skipped background
        cls_boxes = boxes[:, 4*cls_ind:4*(cls_ind + 1)]
        cls_scores = scores[:, cls_ind]
        dets = np.hstack((cls_boxes,
                          cls_scores[:, np.newaxis])).astype(np.float32)
        keep = nms(dets, NMS_THRESH)
        dets = dets[keep, :]
        vis_detections(im, cls, dets, image_name,  thresh=CONF_THRESH)

def save_det(im, class_name, dets, thresh=0.5):
    """Save results with bounding boxes."""
    inds = np.where(dets[:, -1] >= thresh)[0]
    if len(inds) == 0:
        print "WARNING: none bounding box!"
        return im
    font = cv2.FONT_HERSHEY_SIMPLEX
    for i in inds:
        bbox = dets[i, :4]
        rect_start = (bbox[0], bbox[1])
        rect_end = (bbox[2], bbox[3])
        color = (0, 255, 0)
        cv2.rectangle(im, rect_start, rect_end, color, 1, 0)
        cv2.putText(im, class_name, (bbox[0], bbox[1]), font, 1, (255,0,0), 2, cv2.CV_AA)
    #cv2.imwrite("out.jpg", im)
    return im

def test(net, input_images, output_images):
    """Save test images with bounding boxes"""
    print input_images
    for (input_image, output_image) in zip(input_images, output_images):
        if not os.path.exists(input_image):
            print "ERROR: file is not existed!\n"
            exit(-1)
        print "input_image = " + input_image
        im = cv2.imread(input_image)
        # Detect all object classes and regress object bounds
        scores,boxes = im_detect(net, im)
        # Save detections for each class
        CONF_THRESH = 0.8
        NMS_THRESH = 0.3
        for cls_ind, cls in enumerate(CLASSES[1:]):
            cls_ind += 1 # because we skipped background
            cls_boxes = boxes[:, 4*cls_ind : 4*(cls_ind + 1)]
            cls_scores = scores[:, cls_ind]
            dets = np.hstack((cls_boxes, cls_scores[:, np.newaxis])).astype(np.float32)
            keep = nms(dets, NMS_THRESH)
            dets = dets[keep, :]
            im = save_det(im, cls, dets, thresh=CONF_THRESH)
        cv2.imwrite(output_image, im)
        print "output_image = " + output_image

def gen_test_image_list(list_file, input_root, output_root):
    """Generate test image lists"""
    open_file = open(list_file, 'r')
    input_images = []
    output_images = []
    for image_prefix in open_file.readlines():
        image_prefix = image_prefix.strip()
        input_image = os.path.join(input_root, image_prefix + ".jpg")
        output_image = os.path.join(output_root, "result_" + image_prefix + ".jpg")
        input_images.append(input_image)
        output_images.append(output_image)
    return (input_images, output_images)

def parse_args():
    """Parse input arguments."""
    parser = argparse.ArgumentParser(description='Faster R-CNN demo')
    parser.add_argument('--gpu', dest='gpu_id', help='GPU device id to use [0]',
                        default=0, type=int)
    parser.add_argument('--cpu', dest='cpu_mode',
                        help='Use CPU mode (overrides --gpu)',
                        action='store_true')
    parser.add_argument('--net', dest='demo_net', help='Network to use [vgg16]',
                        choices=NETS.keys(), default='vgg16')

    args = parser.parse_args()

    return args

if __name__ == '__main__':
    cfg.TEST.HAS_RPN = True  # Use RPN for proposals

    args = parse_args()

    prototxt = os.path.join(cfg.MODELS_DIR, NETS[args.demo_net][0],
                            'faster_rcnn_alt_opt', 'faster_rcnn_test.pt')
    caffemodel = os.path.join(cfg.DATA_DIR, 'faster_rcnn_models',
                              NETS[args.demo_net][1])

    print args
    if not os.path.isfile(caffemodel):
        raise IOError(('{:s} not found.\nDid you run ./data/script/'
                       'fetch_faster_rcnn_models.sh?').format(caffemodel))

    if args.cpu_mode:
        caffe.set_mode_cpu()
    else:
        caffe.set_mode_gpu()
        caffe.set_device(args.gpu_id)
        cfg.GPU_ID = args.gpu_id
    net = caffe.Net(prototxt, caffemodel, caffe.TEST)

    print '\n\nLoaded network {:s}'.format(caffemodel)

    VOC_ROOT = os.path.join(cfg.DATA_DIR, "VOCdevkit2007", "VOC2007")
    list_file = os.path.join(VOC_ROOT, "ImageSets", "Main", "test.txt")
    input_root = os.path.join(VOC_ROOT, "JPEGImages")
    output_root = os.path.join(VOC_ROOT, "TestResults")
    if os.path.exists(output_root):
        shutil.rmtree(output_root)
    os.mkdir(output_root)
    input_images, output_images = gen_test_image_list(list_file, input_root, output_root)
    test(net, input_images, output_images)

```

输入测试命令：
```
    ./tools/demo-wangxn.py --net zf
```
或者将默认的模型改为zf：
```
parser.add_argument('--net', dest='demo_net', help='Network to use [vgg16]',  
                        choices=NETS.keys(), default='zf')  
```
测试的结果保存到**py-faster-rcnn/data/VOCdevkit2007/VOC2007/TestResults**.

## Trouble Shooting
### 'assert（boxes[:,2]>=boxes[:,0]）.all() issue
出现问题：训练faster rcnn时出现如下报错：
```
File "/py-faster-rcnn/tools/../lib/datasets/imdb.py", line 108, in append_flipped_images
    assert (boxes[:, 2] >= boxes[:, 0]).all()
AssertionError
```

检查自己数据发现，左上角坐标（x,y）可能为0，或标定区域溢出图片.
而faster rcnn会对Xmin,Ymin,Xmax,Ymax进行减一操作, 如果Xmin为0，减一后变为65535
```
# Make pixel indexes 0-based
        # x1 = float(bbox.find('xmin').text) - 1
        # y1 = float(bbox.find('ymin').text) - 1
        # x2 = float(bbox.find('xmax').text) - 1
        # y2 = float(bbox.find('ymax').text) - 1
```

问题解决:
1、修改lib/datasets/imdb.py，append_flipped_images()函数
数据整理，在一行代码为 boxes[:, 2] = widths[i] - oldx1 - 1下加入代码：
```
for b in range(len(boxes)):
  if boxes[b][2]< boxes[b][0]:
    boxes[b][0] = 0
```
2、修改lib/datasets/pascal_voc.py, load_pascal_annotation(,)函数将对Xmin,Ymin,Xmax,Ymax减一去掉.

3、（可选，如果1和2可以解决问题，就没必要用3）修改lib/fast_rcnn/config.py，不使图片实现翻转，如下改为：
```
# Use horizontally-flipped images during training?
__C.TRAIN.USE_FLIPPED = False
```



### 'max_overlaps' issue
使用自己数据集训练Faster-RCNN模型时，如果出现'max_overlaps' issue， 极有可能是因为之前训练时出现错误，但pkl文件仍在cache中。所以解决的方法是删除在py-faster-rcnn/data/cache目录下的pkl文件。

### 中文显示
以py-faster-rcnn的demo.py代码为基础，在demo.py中的修改如下：
1. 指定默认编码：
```
import caffe, os, sys, cv2  
reload(sys)  
sys.setdefaultencoding('utf-8')  
```

2. 设置中文字体：
```
def vis_detections(im, class_name, dets, thresh=0.5):  
    """Draw detected bounding boxes."""  
    inds = np.where(dets[:, -1] >= thresh)[0]  
    if len(inds) == 0:  
        return  

    im = im[:, :, (2, 1, 0)]  
    zhfont = matplotlib.font_manager.FontProperties(fname="/usr/share/fonts/truetype/droid/DroidSansFallbackFull.ttf") #字体  

    fig, ax = plt.subplots(figsize=(12, 12))  
    ax.imshow(im, aspect='equal')  
```
上面的zhfont就是设置的中文字体，由于系统的原因，这个路径不一定相同，所以，用下面的命令确定你的中文字体路径：
```
$ fc-match -v "AR PL UKai CN"
Pattern has 37 elts (size 48)
       	family: "DejaVu Sans"(s)
       	familylang: "en"(s)
       	style: "Book"(s)
       	stylelang: "en"(s)
       	fullname: "DejaVu Sans"(s)
       	fullnamelang: "en"(s)
       	slant: 0(i)(s)
       	weight: 80(i)(s)
       	width: 100(i)(s)
       	size: 12(f)(s)
       	pixelsize: 12.5(f)(s)
       	foundry: "PfEd"(w)
       	antialias: True(w)
       	hintstyle: 1(i)(w)
       	hinting: True(w)
       	verticallayout: False(s)
       	autohint: False(s)
       	globaladvance: True(s)
       	file: "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"(w)
       	index: 0(i)(w)
       	outline: True(w)
       	scalable: True(w)
       	dpi: 75(f)(s)
```

3. 添加参数

还是vis_detections()这个函数，修改：
```
ax.text(bbox[0], bbox[1] - 2,  
                '{:s}'.format(s),  
                bbox=dict(facecolor='blue', alpha=0.5),  
                fontsize=14, color='white', fontproperties = zhfont)
```
那个s就是识别的结果（有中文），color后面添加fontproperties = zhfont

这样，就可以在图像上显示中文了.
