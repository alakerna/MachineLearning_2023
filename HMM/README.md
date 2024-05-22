# Project 3: Information Retrieval using Hidden Markov Model

In this project, you will implement the Hidden Markov Model and the Viterbi algorithm to solve the named entity recognition task on the MITMovie dataset.

## Introduction: Hidden Markov Models (HMM) and Named Entity Recognition (NER)
A hidden markov model (HMM) is a simple probabilistic model which is capable of modeling sequential data.

Specifically in HMM, we assume the __*states*__ of a given system evolve over time according to the Markov property. That is, `p(s_t|s_{t-1},...,s_1) = p(s_t|s_{t-1})`, where `s_t` is the random variable representing the state value at time `t`. What differentiates an HMM from a Markov chain is that we do not get to directly observe these states. So, we call the states *latent* (or *hidden*). Instead, we model that each hidden state `s_t` generates an observation `o_t` according to some probability distribution, `p(o_t|s_t)`. A final ingredient to specify an HMM model is the initial distribution over the first hidden state, i.e., `p(s_1)`. 

**Probability distributions in an HMM**
- Initial distribution `p(s_1)`
- Transition probability `p(s_t|s_{t-1})`
- Emission probability `p(o_t|s_t)`

### Named Entity Recognition (NER)
With the above probability distributions defined, we can perform several inference tasks (e.g., filtering, smoothing, and MPE) as described in the [Lab 6 material](https://colab.research.google.com/drive/1eYazcNvjCPr8GF1JbgLyw04cMUhYFNGs?usp=sharing). In this project, we will focus on the task of identifying the most likely sequence of hidden states given observation data. To this end, we introduce the Named Entity Recognition (NER) task. The following is the definition of the task from [Wikipedia](https://en.wikipedia.org/wiki/Named-entity_recognition):

> Named-entity recognition (NER) (also known as (named) entity identification, entity chunking, and entity extraction) is a subtask of information extraction that seeks to locate and classify named entities mentioned in unstructured text into pre-defined categories such as person names, organizations, locations, medical codes, time expressions, quantities, monetary values, percentages, etc.

An example of such task is described also in the Wikipedia page:
> Original text: Jim bought 300 shares of Acme Corp. in 2006.
> <br> Annotated text: 'Jim'(Person) bought 300 shares of 'Acme Corp.'(Organization) in '2006'(Time).

In the above example, three entities are identified: (1) Jim is a person, (2) Acme Corp. is an organization, and (3) 2006 is a time entity.  You can also notice that this type of natural language data is inherently sequential.  One critical challenge in this task is that an entity could be consisting of more than one word token.  That is, we need to be able to figure out the chunks of word tokens of various lengths, corresponding to predefined named entities.  

#### BIO (Beginning-Inside-Outside) Tagging
To this end, we can set up a dataset labeled with the BIO tags (Beginning-Inside-Outside). Let's take a look at an example from the MITMovie dataset that we use in this project. Consider the following sentence:

> Steve McQueen provided a thrilling motorcycle chase in this greatest of all WW 2 prison escape movies.

In the MITMovie dataset, we want to recognize entities such as `Actor`, `Director`, `Genre`, `Plot`, `Opinion`, etc (detailed description comes in a later part). So, the sentence is labeled as follows:

```
B-Actor	steve
I-Actor	mcqueen
O	provided
O	a
B-Plot	thrilling
I-Plot	motorcycle
I-Plot	chase
I-Plot	in
I-Plot	this
B-Opinion	greatest
I-Opinion	of
I-Opinion	all
B-Plot	ww
I-Plot	2
I-Plot	prison
I-Plot	escape
I-Plot	movies
```
So, you can see that `B-` denotes the beginning of a sequence of tokens corresponding to an entity (like `Actor`), `I-` means that this token is inside the chunk of word tokens representing some entity, and the tokens labeled with `O` are simply irrelevant tokens outside any entity tokens.

### NER with HMM

<img src="https://github.com/jihwan-jeong/figures-repo/blob/master/hmm_mitmovie.svg" width="800">

Now, do you see how this data can be modeled as an HMM?  We can view the BIO tags as the values of a hidden state which produces a word token as an observation. So, in the MITMovie example, `s_1='B-Actor'`, `s_2='I-Actor'`, `s_3='O'`, and so on and so forth. The corresponding sequence of observations is `o_1='steve'`, `o_2='mcqueen'`, `o_3='provided'`. Hence in this dataset, `steve mcqueen` is an instance of the actor entity, while `greatest of all` corresponds to the opinion entity. 

*Note: Although there are more advanced sequential modeling techniques solving the NER task, we are going to limit ourselves to an HMM model in this project. In fact, the same basic ideas employed in HMMs are reused in more complex models, but the story becomes much more complicated and is beyond the scope of this intro course. The key takeaway of this project is to have a grounded understanding of Bayes nets.*

#### NER as most likely Explanation via the Viterbi Algorithm

With the BIO tags representing the hidden states of an HMM and word tokens being the observations from the states, we can perform the following inference task:
> Given a sequence of observations `o_1,...,o_T`, what is the most likely sequence of hidden states (i.e., `s_1,...,s_T`) that could have generated these observations?

The task can be summarized as the following equation:

![](https://latex.codecogs.com/png.latex?\dpi{110}&space;\bg_white&space;\arg\max\limits_{s_1,\dots,s_T}~~p(s_1,\dots,s_T\lvert&space;o_1,\dots,o_T))

The task of identifying the most likely sequence given observations is also called decoding. When the tokens `"steve", "mcqueen", "provided", "a"` are given to us, we want to retrieve the true labels `"B-Actor", "I-Actor", "O", "O"` when we decode this sequence of observations. If correctly decoded as such, we have successfully recognized the named entity `Steve McQueen`.  

The Viterbi algorithm (it's a DP algorithm) can efficiently solve this task. You build a table (often called trellis) of state progression over time step `T`, which has `n` rows and `T` columns when `n` is the cardinality of the state space. Then, you compute 

![](https://latex.codecogs.com/png.latex?\dpi{110}&space;\bg_white&space;\delta_t(j)=\max_{i=1,\dots,n}&space;&space;\big[&space;\delta_{t-1}(i)\cdot&space;p(s_t=j|s_{t-1}=i)\cdot&space;p(o_t|s_t=j)&space;\big])

to fill in the trellis for all `j=1,...,n`, starting from `t=1`. 

One more thing to note is that we need to know the parameters of the conditional probability tables `p(s_t|s_{t-1})` and `p(o_t|s_t)`. A somewhat advanced algorithm to learn these parameters is the [Baum-Welch](https://en.wikipedia.org/wiki/Baum%E2%80%93Welch_algorithm) algorithm. This algorithm finds a local maximum of the parameters that maximize the marginal probability of observations. The Baum-Welch algorithm is useful when you have access only to the observations but not to the hidden states. Fortunately for us, the MITMovie dataset contains the BIO label information.  Hence, we opt for a much simpler parameter estimation approach: we are going to compute the parameters simply based on the counts, which is essentially the maximum likelihood estimation (MLE) of the parameters.  More concretely,

![](https://latex.codecogs.com/png.latex?\dpi{110}&space;\bg_white&space;p(s'=j|s=i)&space;=&space;\frac{\sum&space;I(s'=j,s=i)}{\sum&space;I(s=i)},\qquad&space;p(o=k\lvert&space;s=i)=\frac{\sum&space;I(o=k,s=i)}{\sum&space;I(s=i)})

where we have omitted the subscript `t` since we assume that the state transitions and the observation emissions are time-independent.

For more information, consult the project slides.

## The MITMovie Dataset
As alluded to early on, we use the MITMovie dataset for this project. You can find the train and test sets in the [data folder](./data). The train set consists of 7816 documents and 10987 unique tokens while the test set consists of 1953 documents and 5786 unique tokens. Here, a document could consist of a single or a few sentence(s), and a document is separated from other documents by a blank line. There are a total of 12 named entities in the dataset: 

> Actor, Award, Character_Name, Director, Genre, Opinion, Origin, Plot, Quote, Relationship, Soundtrack, Year

However, you can select to include only a subset of these entities by providing the names of the entities in the [data/problem.txt](./data/problem.txt) file between "Entity" and "EndEntity" as below:

```angular2html
Entity
Actor Award Character_Name
EndEntity
```
If an entity is omitted in the text file, the corresponding tokens will have `"O"` as their labels. 

### Performance Metrics
How should we evaluate the performance of our HMM in the context of the NER task? For evaluation, we compare the decoded BIO tag for each word token to its true BIO tag. Then, the **accuracy** of the model is defined as `[# of correctly labeled tokens] / [# of tokens]`. 

Notice that the accuracy of a model gives credit to all correctly decoded `"O"` tokens. However, we care more about how many of the named entities have been correctly retrieved. To better account for this, we use **precision**, **recall**, and **F1** score per entity. 

In order to understand what these metrics evaluate, you need to understand the following concepts defined for binary classification:
- True positive (TP)
- False positive (FP)
- True negative (TN)
- False negative (FN)

For example, let's say there is only a single named entity `"Actor"` and all the other tokens are `"O"`. Suppose your Viterbi returns `I-Actor` as the decoded label for `"mcqueen"` token when the true BIO tag of this token is also `I-Actor`. Then, this instance corresponds to TP because your model "correctly" and "positively" recognized this token as the named entity. On the other hand, if the model says the token `"thrilling"` is an `Actor` entity, this is an "incorrect" "positive" retrieval, which is an example of FP. Tokens that correspond to the named entity but decoded as `"O"` are classified as FN. Finally, there can be the case when a token with the `"O"` tag is correctly decoded to have the `"O"` tag, which corresponds to TN. 

The precision is defined as `(# TP) / (# TP + # NP)`. That is, we compute the ratio of the number of correctly decoded tokens to the total number of tokens that are positively identified as a named entity. The recall evaluates `TP / (TP + FN)`, which is the ratio of 
the number of correctly identified named entity tokens among all named entity tokens. The F1 score is the harmonic mean of the precision and recall.  

Since there are 12 named entities in the MITMovie dataset, we *microaverage* the performance. That is, we will consider per token classification. 

For more on the performance metrics, refer to the project slides and the links below: 
- [Accuracy](https://en.wikipedia.org/wiki/Accuracy_and_precision#In_binary_classification)
- [Precision and Recall](https://en.wikipedia.org/wiki/Precision_and_recall)
- F1 score (the balanced [F-score](https://en.wikipedia.org/wiki/F-score))
- [Microaveraging](https://nlp.stanford.edu/IR-book/html/htmledition/evaluation-of-text-classification-1.html)

## Project description

The probabilistic inference task you will perform in this project is the MPE task, in which you need to figure out the most likely sequence of the BIO tag values given word observations from the MITMovie dataset. This task breaks down into the following two steps:

1. Fitting the HMM model parameters with the train set

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; You first need to learn the parameters of the transition and emission probability tables. This can be achieved by counting occurrences of states and observations as described earlier.

2. Decoding the documents in the test set

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; Once you have the parameters learned, you need to run the Viterbi algorithm to decode the most likely sequence of hidden state values corresponding to a sequence of observations. 

<br>

You will be implementing these two steps in this project by following through Question 1 and Question 2 below. 

**Note**: notice that the states are implemented as enum values in [State.java](./src/main/java/model/State.java). The state IDs of your implementation **MUST MATCH** the IDs predefined in this file. This is very important as the autograder will use these IDs during evaluation. You can use the static method `getStateFromID` to get the `State` instance given an ID and use the instance method `getID` to get the ID of a given `State` instance.

### Question 1. Fit the Parameters of the HMM [3 pts]
Complete the `fit` method in [src/main/java/model/HMM.java](./src/main/java/model/HMM.java). 

Note that the `initialize` method is called at the beginning. This method sets the values of some useful variables. Then, the arrays (`_stateVisitCounts`, `_obsVisitCounts`, `_state2StateVisitCounts`) to which you should record the corresponding visit counts are instantiated. Also, two probability tables (transition and emission) are instantiated.

You should use the `setValue` method to set the value of an entry of a CPT. Each row of a CPT should sum up to 1, and this is ensured by calling `normalize()` after setting the values of the CPT.

Since the probabilities are computed based on the visit counts, remember to update the visit counts of states and words first by going through each and every pair of (state, word) in the train set.

**(a) [1.5 pts]** Fit the parameters of the transition probability.
 
**Note 1**: there may be no recorded transitions from `s=i` to `s'=j` in the train set. You could then simply set the corresponding probability `P[i][j]=0`, which is not a good idea because you will later evaluate the log of this value during `decode`. To prevent this from happening, set `P[i][j]=EPSILON` (`EPSILON` is defined in Model.java) whenever there is zero occurrence of `s'=j, s=i`. A few exceptions are explained below. 

**Note 2 (exceptions to Note 1)**: for the `START` state, it is correct that there should be no transitions into the state. Therefore, `P[i][START_STATE_ID]=0` should hold for all states `i`.  Similarly, no transitions should go out from the `END` state, so `P[END_STATE_ID][j]=0` should hold for all states `j`. Additionally, set `P[START_STATE_ID][END_STATE_ID]=0` since the corresponding transition cannot happen for any non-empty document.  

**(b) [1.5 pts]** Fit the parameters of the emission probability.

**Note 3**: the occurrence count of a word with the index `k` at a specific state of index `i` could be 0. Similar to Note 1, set `P[i][k]=EPSILON` to prevent evaluating the log of zero. However, there should be no observations generated by the `START` and `END` states, so simply set all `P[i][k]=0` if `i==START_STATE_ID` or `i==END_STATE_ID`.

### Question 2. Viterbi [4 pts]
Complete the `decode` method in [src/main/java/model/HMM.java](./src/main/java/model/HMM.java) by implementing the Viterbi algorithm. 

Note that this method is called within the `decodeAllDocs` method in [Model.java](./src/main/java/model/Model.java) which takes in `testLoader` as an argument. The argument of the `decode` method is a single document, i.e., an ArrayList of word tokens. The method should return `mostLikelyPath`, which is an array of Integers of length `T+2` when the document contains `T` tokens. This is to account for the `START` and `END` states that are prepended and appended, respectively.

An element of `MostLikelyPath` is the decoded state ID of the corresponding token (observation). Naturally, `MostLikelyPath[0]` should be the ID of the `START` state and `MostLikelyPath[T+1]` should be the ID of the `END` state. You need to instantiate the array with the proper length and fill in all elements with the decoded state IDs.

**Note 4**: a new word token in the test set would lead to the zero emission probability from all states. To avoid evaluating the log of zero, treat the emission probability of an unseen word from a state `i` simply as `EPSILON`.

### Question 3. Competitive Portion [3 pts]

Consider an enhancement to the HMM (based on lecture, the book, and/or any external resources you find) and write it in [src/java/main/model/HMMEnhanced.java](./src/java/main/model/HMMEnhanced.java). The number of adjustments you may make for enhancement is not limited. 

To implement the enhancements and also for the purpose of auto-grading, make sure you define all the necessary parameters of the model in the `HMMEnhanced.java`. You can define them as static final variables and use them throughout the file. Just remember not to use anything that wouldn't be available for us when we auto-grade your code.  

Your competitive portion will be graded based on the performance. Specifically, we will microaverage the precision and recall of your enhanced model across all 12 entities on the test set. Then, we will compute the F1 score using this precision and recall, which will be used for grading. You will get a score in the range `[0, 3]` scaled linearly with `0` points for achieving a F1 score that is smaller than our solution and the full `3` points for the highest F1 score among correct submissions. You can see how good your model is by looking at the average F1 value in the console outputs. 

To discourage your attempt to over-engineer your solution such that it overfits the provided test data, we will use a different test set that is generated in the following way. First, we combine the train and test set. Then, we split the corpus into the train set with `7816` documents and the test set with `1953` documents, using a fixed random seed that is not going to be revealed to students.

Additionally, if your code doesn't terminate within `20` seconds, you will get `0` grade for the competitive portion.

In [enhancement.txt](./enhancement.txt), briefly describe the enhancement(s) you implemented in the HMMEnhanced.java file. Otherwise, you will get `0` grade.

FYI, it takes ~2.5 seconds to run our solution code, which produces the F1 score of `0.8595` for the distributed test set.   


**Note 5**: Do not modify abstract classes or other classes we provide -- we rely on these exact class definitions for autograding. You may add additional helper classes and you may add as many additional methods to your solution classes as you want.

## How to run:
You can run the code using the [Main.java](./src/main/java/Main.java) file. The performance metrics of your model will be printed out based on the test set. 

In the file, we go through the following steps:
1. Paths to the datasets are defined.
```java
String trainDataPath = "data/trivia10k13train.bio";
String testDataPath = "data/trivia10k13test.bio";
```
2. `trainLoader` is instantiated, during which the training data are loaded and stored.
```java
DataLoader trainLoader = new DataLoader(trainDataPath);
```
3. The `HMM` model object is instantiated. Then, you call the `fit` method.
```java
HMM model = new HMM();
model.fit(trainLoader);
```
4. The test `DataLoader` object is instantiated. Then, you call the `decodeAllDocs` method to compute the most likely paths of the documents in the test set. The results are saved in `decodedPath`. 
```java
DataLoader testLoader = new DataLoader(testDataPath, false);
HashMap<Integer, Integer[]> decodedPath = model.decodeAllDocs(testLoader);
```
5. Finally, compute and print out the final performance metrics. 
```java
Util.computePerformance(decodedPath, testLoader, null);
```

## Test your implementation

Run the [Tests.java](./src/test/java/Tests.java) file to see if your HMM.java implementations are correct. 

## Optional reading

- Laplace smoothing (see [Wikipedia](https://en.wikipedia.org/wiki/Additive_smoothing))
- Shrinkage (mixture models) (see [Freitag & McCallum (1999)](https://www.aaai.org/Papers/Workshops/1999/WS-99-11/WS99-11-006.pdf))