# Change Log

## [0.7.0] - FIXME

### add

- standard rnn
- Gated Recurrent Unit(GRU)

### change

- LSTM was moved to src/prism/nn/rnn/
- encoder-decoder-lstm was moved to src/prism/nn/encoder-decoder/

### rename

- sequential-output was renamed to forward (lstm)

## [0.6.0] - 2017-6-1

### add

- encoder-decoder model formally (LSTM only)
- tentatively add batch normalization of feedforward

### change

- trivial changed architecture of each models
- use clojure.core.matrix.random for sampling

### remove

- matrix to be more readable

## [0.5.1] - 2017-5-21

### add

- error-function for multiclass classification
- add LSTM demo for multiclass classification

## [0.5.0] - 2017-5-20
### Changed

- add CHANGELOG.md (this file)
- now prism uses clojure.core.matrix as default matrix implementation
- add orthogonal-initialization for recurrent connection

