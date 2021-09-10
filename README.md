# credit-card-processor

## Design

Overall design is to consume a stream of commands (line by line) and execute them serially. A specification for the 'commands' was written and the string input is conformed to it. If the input does not conform to spec, we simply ignore the command.

The commands are executed via a dispatch mechanism, `command-interface`, which provides functional polymorphism and an open system for extension.

All commands were classfied as either `account/create` or `account/update` and applied against a backing store - which intially is just a hashmap of all accounts. No history is stored in the backing store, just summary information. 

The account updates were desined to be applied transactionally - i.e. compare and swap - where the account actions are executed by an `executor`. The executor validates the current state of the account and if it is valid applies the update by generating a new account record with the update applied. It then validates this new record before applying it to the store. The accounts store returned by the executor can then be used for a transactional update, i.e. compare & swap, scheme in a parallel or concurrent execution environment.

Double validation is necessary to prevent an update command from making an invalid account valid althought this technically was not possible from just `charge` and `credit` actions. 

One requirement mentioned a charge should not put the account over the limit. This was done in post-update validation in order to consolidate the validation logic - not have a piece of it sitting inside a function. Consolidating validations via a specefication also mean we can do deep levels of generative testing, instrumentation and general validation.

### Language Choice

Clojure was chosen for it's quick prototyping ability and for it's deep testing and contract specification / validation features. Although it is a 'dynamic' language it allows the developer to opt-in to types and compile time as well as run-time correctness checking in the critical areas where it makes the most sense. 

The challange mentioned building on top of it and the concurrency primitives as well as the functional nature, particularly its focus on immutablity, make it almost trivial to go from a single-threaded application to a thread-safe reacitve, message based or any other concurrent / parallel / distributed compute model.

This challange seemed to focus on testing and design which seemed a good fit for the tools Clojure has to offer.

## Installation

* Requires `JDK 1.8+` on path or set in `JAVA_HOME`
* Requires `clojure` cli tools: See [Clojure: Getting Started](https://www.clojure.org/guides/getting_started) for installation instructions

## Usage

Run the project directly:

    $ clojure -m mangosmoothie.credit-card-processor data.txt

    $ clojure -m mangosmoothie.credit-card-processor < data.txt

Run the project's tests (currently runs ~3000 generative tests):

    $ clojure -A:test:runner

## Assumptions

* All "names" (ex: Quincy) are single word unique identifiers - one name is associated with one and only one card number / account.
* Input data "fields" are separated with a single space
* You can re-add an account to make a valid account for the "name"
