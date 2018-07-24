# Prism

[![Build Status](https://travis-ci.org/Jah524/prism.svg?branch=master)](https://travis-ci.org/Jah524/prism)

Prism is a neural network library for natural language processing written in pure Clojure.
This library get you a distributed representation of words.

![Visualization with t-sne](https://s3.amazonaws.com/prism-archive/source/embedding.png)

Now prism is ready to work with following models.

- Word2Vec by skip-gram with negative-sampling
- RNNLM with negative-sampling
- Skip-Thought

This library also includes some basic neural network model (e.g. feedforward, RNN(LSTM, GRU), encoder-decoder(LSTM, GRU)).
See demonstration section and [examples](/src/examples) for more details.

And prism also provides visualization tool that makes figure like above one.
See [wiki](https://github.com/Jah524/prism/wiki/Visualization).

## Usage

`git clone https://github.com/Jah524/prism.git` and `lein repl`.
You can find how to do with prism at [wiki](https://github.com/Jah524/prism/wiki)

If you want to work on trained model in your project, add following dependency to your `project.clj`,

```
[jah524/prism "0.8.8"]
```

## models for NLP

### Word2vec

```clojure

;make word embeddings
(require '[prism.nlp.word2vec :as w2v])
(def model (w2v/make-word2vec "your-training-path.txt" "model-save-path.w2v" 100 {:workers 2}))

;use word embeddings
(require '[prism.util :as util])
(def em (util/load-model "your-save-path.w2v.em"))
(get em "word") ;=> #vectorz/vector [0.06990837345068174,-0.09570045605373989 ....]

(util/similarity (get em "word1") (get em "word2") true) ;=> 0.03489215427695168

(require '[clojure.core.matrix :as m])
(m/set-current-implementation :vectorz)
```

- example using [pretrained model](https://s3.amazonaws.com/prism-archive/pretrained-model/1-billion-word-language-modeling-benchmark_200h_ns5_min10.w2v)

```clojure
(require '[prism.util :as util])
(require '[prism.nlp.word2vec :as word2vec])
(require '[clojure.core.matrix :as m])
(m/set-current-implementation :vectorz)

(def em (util/load-model "https://s3.amazonaws.com/prism-archive/pretrained-model/1-billion-word-language-modeling-benchmark_200h_ns5_min10.w2v.em"))
;; it takes a minutes (222.0MB)

; or
; download the model and
(def em (util/load-model "your path to pretrained model"))


(def target-list ["Japan" "Tokyo" "China" "Beijing" "Bangkok" "Thai" "Singapore" "France" "Paris" "Italy" "Rome" "Spain" "Madrid"])

(def v1 (m/add (m/sub (get em "Japan") (get em "Tokyo")) (get em "Paris")))
(word2vec/most-sim em v1 target-list)
;=> ({:word "France", :sim 0.7725763} {:word "Italy", :sim 0.6931164} {:word "Spain", :sim 0.6633791} {:word "Paris", :sim 0.64103466} {:word "Rome", :sim 0.49774215})

(def v2 (m/add (m/sub (get em "Japan") (get em "Tokyo")) (get em "Beijing")))
(word2vec/most-sim em v2 target-list)
;=> ({:word "China", :sim 0.97199464} {:word "Beijing", :sim 0.8846489} {:word "Japan", :sim 0.7798172} {:word "Italy", :sim 0.44583768} {:word "Singapore", :sim 0.41647854})

```

- See [Word2vec](https://github.com/Jah524/prism/wiki/Word2Vec) for more details.

### RNNLM

```clojure
(require '[prism.nlp.rnnlm :as rnnlm])
(def rnnlm (rnnlm/make-rnnlm "your-training-path" "model-save-path.rnnlm" 100 :gru {:workers 2 :negative 5}))

(require '[prism.util :as util])
(def rnnlm (util/load-model "your-save-path.rnnlm"))

;; you can take distributed representation of text or phrase
(rnnlm/text-vector rnnlm ["word1" "word2" "word3"]) ;=> #vectorz/vector [0.5559875183548029,0.6338452816753448,0.49570920352227194 ...]

;; and resume train, model-path represents your-save-path.rnnlm
(rnnlm/resume-train "your-training-path" "model-path.rnnlm" {:workers 4})
```

- See [RNNLM](https://github.com/Jah524/prism/wiki/RNNLM) for more details.

### Skip Thought

``` clojure
(require '[prism.nlp.skip-thought :as st])

(def m (st/make-skip-thought "your-training-path" "your-embedding-path" "model-save-path.st" 200 100 100 :gru {:workers 4 :interval-ms 10000}))

;; get encoder
(def en (st/get-encoder m))

;; get words vector
(st/skip-thought-vector en ["word1" "word2" "word3"]) ; => #vectorz/vector [0.08737680011079421,0.07707453000181463, ...]

;; save and load encoder
(require '[prism.util :as u])
(u/save-model en "output-path.st.encoder")
(def en (u/load-model "output-path.st.encoder"))
```

- See [Skip-Thoght Vectors](https://github.com/Jah524/prism/wiki/Skip-Thought) for more details.

## Visualization

Move on to your console.

```
git clone https://github.com/Jah524/prism.git
lein run -m server.handler word2vec `path-to-your-embedding` --port 3003
```
go to `127.0.1.1:3003` on your browser and you can see visuallization tool


![example1](https://s3.amazonaws.com/prism-archive/source/example1.png)

### Items

First, you put together items you want visualize.
Next, give them by text file or input manually.
You can try to use [example.txt](https://s3.amazonaws.com/prism-archive/source/example.txt) (body has took from [clojure.org](https://clojure.org/)).

### t-sne parameters

We use t-sne for dimensionality reduction to visualize.
Perplexity and iteration are needed at least.
The perplexity should not too much bigger or smaller, and iterations also should not be too small.
You can get to know more about parameters in [T-SNE-Java](https://github.com/lejon/T-SNE-Java).

You ready?
Then you just put green button below.

### Result

![example2](https://s3.amazonaws.com/prism-archive/source/visualization-result.png)

You might get result figure top of the page.
Keep in mind we use t-sne(non-deterministic) so that a result is different by trials.
When something doesn't work, check your messages on console.

Note, we use [plotly.js](https://github.com/plotly/plotly.js) for visuallization.



## Basic neural networks

### Feed Forward

- approximation of sin function with 3 hidden units

```
lein run -m  examples.feedforward.sin3
```

### RNN

you can choose rnn model from #{"standard" "lstm" "gru"} for each example

- with dense input

```
lein run -m examples.rnn.simple-prediction standard
```

- with sparse inputs

```
lein run -m examples.rnn.sparse gru
```

- multi class classification

```
lein run -m examples.rnn.multi-class lstm
```

## License

Copyright © 2017 Jah524

Distributed under the Eclipse Public License either version 1.0

