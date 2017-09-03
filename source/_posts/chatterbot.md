---
title: 构建自己的聊天机器人（及如何接入微信）
date: 2017-9-3 10:46:25
categories:
  - AI
  - IOT
tags:
  - 人工智能
  - IoT

---

## Chatbot的开发框架

目前聊天机器人的云服务在各大公司中都有自己的框架，例如Facebook, 微软, Google以及IBM的Watson等。开源的框架中，基于机器学习的聊天机器人不多。其中，ChatterBot算是比较简洁（换种说话就是功能还比较弱）的一个，项目活跃, 文档清晰，代码也算干净利落。

具体说来，ChatterBot是一个基于机器学习的聊天机器人引擎,构建在python上,可以从已有的对话中学习, 该项目的设计允许它接入任何语言。

其特性：
- 一个未经训练的ChatterBot机器人,并没有与用户交谈所需的知识。
- 每当用户输入一句话，机器人将存下它，同时也存下答复的句子。 
- 随着机器人接受的输入的增加，它能够回答的问题的数量和准确度都会相应提升.

实现原理：
- 首先从已知句子中匹配出与用户输入最相近的句子（如何衡量相近, 大家可以想想）；
- 之后找到最有可能的回复，那么如何得出最有可能的回复呢？由所有和机器交流过的人们，对这个输入问题（匹配过的）的各个回答的频率决定；

## 环境搭建

### Python3
- By default, python3.5.2 is installed in Ubuntu 16.04
- sudo apt-get install -y python3-pip
- pip3 install package_name

### Installing ChatterBot from source
- git clone https://github.com/gunthercox/ChatterBot.git
- pip3 install ./ChatterBot

### Checking the version of ChatterBot that you have installed
```
python3 -m chatterbot --version
0.7.6
```
我用的是当前最新的版本0.7.6。
