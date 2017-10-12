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

## 训练模型
我们采用ZF模型进行训练，输入如下命令：
```
    ./experiments/scripts/faster_rcnn_alt_opt.sh 0 ZF pascal_voc  
```
## 测试模型

将训练得到的**py-faster-rcnn/output/faster-rcnn-alt-opt/voc_2007_trainval/ZF_faster_rcnn_final.caffemodel**模型拷贝到**py-faster-rcnn/data/faster/rcnn_model**

## Trouble Shooting
### 'max_overlaps' issue
使用自己数据集训练Faster-RCNN模型时，如果出现'max_overlaps' issue， 极有可能是因为之前训练时出现错误，但pkl文件仍在cache中。所以解决的方法是删除在py-faster-rcnn/data/cache目录下的pkl文件。
