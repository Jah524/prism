# Prism

[![Build Status](https://travis-ci.org/Jah524/prism.svg?branch=master)](https://travis-ci.org/Jah524/prism)

Prism is a handy neural network library for natural language processing written in pure Clojure.
This library get you a distributed representation of words.
Now prism is ready to work with following models.

- Word2Vec by skip-gram with negative-sampling
- RNNLM with negative-sampling

This library also includes some basic neural network model (e.g. feedforward, LSTM).
See demonstration section and [examples](/src/examples) for more detail.

## Usage

Add following dependency to your `project.clj`.

```
[jah524/prism "0.2.1"]
```

### Word2Vec

```
(use 'prism.nlp.word2vec)

(make-word2vec your-training-path model-save-path 100 {:workers 4})
;; above exmaple specifies hidden size as 100 and learn your-training-path with 4 workers.
;; your-trainig-file should have tokenized lines.
;; once learning finished, you can see learned model at model-save-path with .w2v and .w2v.em extensions.
;; you can use your learned embedding as following steps.

(use 'prism.util)
(def em (load-model your-save-path.w2v.em))

;; then you can get word embedding.
;; note: embeddings are represented as float-array.
(word2vec em "word")
(vec (word2vec em "word"))

```

### RNNLM

```
(use 'prism.nlp.word2vec)

(make-rnnlm your-training-path model-save-path 100 {:workers 4}
;; see Word2Vec section (above) to get to know about these arguments and parameters
;; RNNLM model has .rnnlm extenstion

(use 'prism.util)
(def rnnlm (load-model your-save-path.rnnlm))

;; you can take distributed representation of text or phrase
(text-vector rnnlm ["word1" "word2" "word3"])

;; and resume train, model-path represents your-save-path.rnnlm
(resume-train your-training-path model-path 100 {:workers 4})
```


## Demonstration

### Feed Forward

- sin approximation with 3 hidden units

```
lein run -m  examples.feedforward.sin3
```

### LSTM

- with dense input

```
lein run -m examples.lstm.simple-prediction
```

- with sparse inputs

```
lein run -m examples.lstm.sparse
```

## License

Copyright © 2017 Jah524

Distributed under the Eclipse Public License either version 1.0

