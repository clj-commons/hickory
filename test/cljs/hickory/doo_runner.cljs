(ns hickory.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [hickory.test.convert]
            [hickory.test.core]
            [hickory.test.hiccup-utils]
            [hickory.test.render]
            [hickory.test.select]
            [hickory.test.zip]))

(doo-tests 'hickory.test.core
           'hickory.test.convert
           'hickory.test.hiccup-utils
           'hickory.test.render
           'hickory.test.select
           'hickory.test.zip)

