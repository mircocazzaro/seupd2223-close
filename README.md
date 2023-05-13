# Search Engines (SE) - LongEval CLEF 2023

---
### objective ###
The objective of this repository is to propose an information retrieval system that can effectively handle changes over time, specifically focusing on the temporal evolution of Web documents. The proposed system aims to adapt to the dynamic nature of the web by considering evolving document and query sets. To accomplish this, we leverage the [Longeval](https://clef-longeval.github.io/tasks/) Websearch collection, a comprehensive dataset provided by Qwant, a commercial search engine. This collection encompasses a diverse corpus of web pages, user queries, and user interactions, allowing us to capture and reflect the changes in the web landscape.

This repository is developed for the [Search Engines](https://iiia.dei.unipd.it/education/search-engines/) course.

This repository is carried out by groups of students and consists in participating to one of the labs organized yearly by [CLEF](https://www.clef-initiative.eu/) (Conference and Labs of the Evaluation Forum).

### Group members ###

| Surname       | Name       | ID       |
| ------------- |------------|----------|
| Antolini	| Gianluca	    | 2080960	       |
| Boscolo Cegion		| Nicola	    | 2074285	 |
| Cazzaro		| Mirco	     | 2076745  |
| Martinelli	| Marco	     | 2087646	 |
| Safavi        | Seyedreza	 | 2071558	       |
| Shami		| Farzad		   | 2090160	 |

---
## Project Description ##
Our approach involves using the training data provided by Qwant search engine, which includes user searches and web documents in both French and English. We believe that this data will enable our system to better adapt to changes in user search behavior and the content of web documents.

### Organisation of the repository ###

The project is developed mainly in Java, with the addition of some Python scripts for performing the expansion of the queries and for the re-ranking of the retrieved documents for each topic. The overall structure is as follows:

* `code`: this folder contains the source code of the developed system.
* `runs`: this folder contains the runs produced by the developed system.
* `results`: this folder contains the performance scores of the runs.
* `homework-1`: this folder contains the report describing the techniques applied and insights gained.
* `homework-2`: this folder contains the final paper submitted to CLEF.
* `slides`: this folder contains the slides used for presenting the conducted project.
---

## How to run and use the codes? ##

* Before any attempt, make sure you have the **collection** and **topics** files available in your system.
* If you want to use the query expansion put your open-api key in the python script and run it. The script will create a file named "result.json" which will be used by the searcher.
* If you want to use the Re-ranking method, you should run on the system that supports `pytorch cuda` version. (for now it is not supported on the `macOS system with Apple Silicon chip`), and pass the [sbert](https://huggingface.co/sentence-transformers) model to the searcher. if your model is exists in `dl4j` it automatically download it, otherwise you should download it and convert it to `torch-script` and put it in somewhere and use path in `re-ranker` class.

#### Running using CLI: ####
We provide here in the folder final_jar_executable a jar executable version of our program
that automatically creates its own working environment and change the parameters based on your needs on the `CloseSearchEngine.java`. To run it follow these steps:

* Open the command line and change directory to where the project folder and build project by running `mvn clean install` command.
* Run the following command passing the correct parameters:
```
java -jar close-1.00-jar-with-dependencies.jar <collection path> <topic path> <index path>
```
----

*Search Engines* is a course of the

* [Master Degree in Computer Engineering](https://degrees.dei.unipd.it/master-degrees/computer-engineering/) of the  [Department of Information Engineering](https://www.dei.unipd.it/en/), [University of Padua](https://www.unipd.it/en/), Italy.
* [Master Degree in Data Science](https://datascience.math.unipd.it/) of the  [Department of Mathematics "Tullio Levi-Civita"](https://www.math.unipd.it/en/), [University of Padua](https://www.unipd.it/en/), Italy.

*Search Engines* is part of the teaching activities of the [Intelligent Interactive Information Access (IIIA) Hub](http://iiia.dei.unipd.it/).


----
### License ###

All the contents of this repository are shared using the [Creative Commons Attribution-ShareAlike 4.0 International License](http://creativecommons.org/licenses/by-sa/4.0/).

![CC logo](https://i.creativecommons.org/l/by-sa/4.0/88x31.png)

