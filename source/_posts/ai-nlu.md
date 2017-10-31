---
title: IoT - 人工智能
date: 2017-8-19 20:46:25
categories:
  - AI
tags:
  - AI
  - NLU
  - IoT

---

## NLU

rasa NLU is an open source tool for intent classification and entity extraction.

Rasa Core takes in structured input: intents and entities, button clicks, etc., and decides what your bot should do next.

![](https://lastmile-rasa-dm.readthedocs-hosted.com/en/latest/_images/rasa_arch.png)


Rather than writing a bunch of if/else statements, a Rasa bot learns from real conversations. A probabilistic model chooses which action to take, and this can be trained using supervised, reinforcement, or interactive learning.



## The Big Differences: IoT Versus Traditional System Data and Architecture

![](https://osswangxining.github.io/images/illustration-iot.svg)

| IOT DATA AND ARCHITECTURE  |  TRADITIONAL SYSTEM DATA AND ARCHITECTURE |
|---|---|
| **IoT data is distributed.** In any given IoT scenario, data is distributed among devices, sometimes thousands of devices, that are often geographically dispersed. Compute and processing power must often take place on network edges.	  | **Application data is created and stored centrally.** Each application and system creates and stores its own data. Data from disparate applications is then aggregated and physically moved to a central location, such as an enterprise data warehouse, for analysis.  |
| **IoT data volume is increasing exponentially.** The volume of data in IoT scenarios might start small, but with devices creating data non-stop, 24 hours a day, it doesn’t take long before IoT data volumes become truly massive. This requires a data management and analytics stack that scales to IoT data volumes.	  | **Traditional application data growth is linear.** Most of the data associated with traditional enterprise applications is created manually. This means the volume of traditional applications data, while growing, is not growing at nearly the pace of machine-generated data. Traditional storage and analytics technologies suffice.  |
| **Real-time and predictive analytics are required on IoT data.** In order to deliver personalized services to customers and adapt equipment operations to maximize efficiencies, IoT data must be analyzed as it is created and be able to trigger corresponding actions.	  | **Rear-view mirror analytics and reporting hinder teams’ ability to be proactive.** Traditional applications and systems are often monolithic, containing a web of components that capture data but make it difficult to find and troubleshoot issues in real time. Analytics takes place well after data is created and provides largely backwards-looking views of events and operations.  |
| **IoT requires event-driven architecture.** An event-driven architecture takes advantage of microservices and allows applications to notify each other of changes in state as they occur and trigger corresponding actions.	| **Traditional architecture is passive.** Traditional application architectures are monolithic and don’t support real-time event notification. |
| **IoT usage will change.** The one constant in any IoT scenario is change. Data scientists and application developers are always looking for new and innovative IoT use cases, which means they need to continuously adapt existing applications and build new applications using Agile methodologies.	| **Application use is fairly static.** Traditional enterprise applications are developed using a waterfall approach to meet existing needs. Applications are rarely updated or changed to meet changing business demands, and new applications take months, sometimes years, to develop and deploy to production. |
