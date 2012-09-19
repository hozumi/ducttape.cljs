# Ducttape.cljs

Ducttape.cljs is a ClojureScript micro MVC framework and suggests a clear skeleton of web application.
As substantial code is so tiny, Ducttape.cljs may be more like convention, agreement or my best practice than a framework.
Currently Ducttape.cljs depends on jQuery.

<img src="https://github.com/downloads/hozumi/ducttape.cljs/ducttape_arch.png" alt="ducttape.cljs architecture" title="ducttape.cljs architecture" align="center" />

### Data Shelf
Data shelf is model minus behavior, i.e. data holder.
An important point is that data shelf contains only a copy of data in the server.
This means that data shelf doesn't contain uncertain data, which doesn't exist in the server.

You need to prepare a data shelf as following.

```clojure
(def data (atom {}))
```

We will put all the data retrieved from the server into this atom, but you can also prepare multiple atoms to group data by kind if you want.
Typical data shelf will contain following nested hashmap.

```clojure
{:messages
 {1 {:id 1 :name "john" :body "Hello" :date 1347875638606},
  2 {:id 2 :name "mike" :body "Hi" :date 1347875672937},
  3 {:id 3 :name "sarah" :body "Hi" :date 1347875687430}},
 :shops
 {492 {:id 492 :tel "1234" :address "abcd 1 2"},
  530 {:id 530 :tel "2345" :address "efgh 3 4"}},
 :login-user
 {:userid "adam" :lang "en"}}
```

You can arrange above structure as you like.

### DOM Shelf
DOM shelf is view minus behavior, i.e. dom holder.

You need to prepare a dom shelf as well as data shelf.

```clojure
(def doms (atom {}))
```

Typical dom shelf will contain following dom mappings.

```clojure
{:signin-view {:el #<[object HTMLDivElement]>,
               :form-el #<[object HTMLFormElement]>,
               :id-el #<[object HTMLInputElement]>,
               :password-el #<[object HTMLInputElement]>,
               :submit-el #<[object HTMLButtonElement]>},
 :message-view
 {1 {:el #<[object HTMLDivElement]>,
     :name-el #<[object HTMLDivElement]>,
     :date-el #<[object HTMLDivElement]>,
     :body-el #<[object HTMLDivElement]>},
  2 {:el #<[object HTMLDivElement]>,
     :name-el #<[object HTMLDivElement]>,
     :date-el #<[object HTMLDivElement]>,
     :body-el #<[object HTMLDivElement]>}},
 :shop-view
 ...}
```

A key point is that doms are grouped by logical model.
Although Ducttape.cljs doesn't force you to use any specific DOM manipulation library, I recommend [my modified version of crate](https://github.com/hozumi/crate-bind) for DOM creation because it can make a hashmap like the above easily.

### function

Let's look at an example of dom creation function.

```clojure
(ns myapp
  (:require [ducttape :as dt]
            [crate-bind.core :as crateb]))
```

```clojure
(defn message-view-init [{:keys [message]}]
  (let [{:keys [id name body date]} message
        {:keys [el] :as binds}
        (crateb/build
         [:li.message.normal
          [:div.message-meta
           [:span.message-id :id-el id]
           [:span.message-user :user-el name]
           [:span.message-date :date-el (js/Date. date)]
           [:span.message-edit-button :edit-el "edit"]]
          [:div.message-body :body-el body]])]
    (dt/delegate el [["mouseenter .message-body" message-view-popup {:dom binds}]
                     ["click .message-edit" message-view-edit-toggle {:dom binds}]])
    (.appendTo (js/$ el) "#wrapper")
    (swap! doms assoc-in [:message-view id] binds)))
```

The name of a function is `message-view-init`.
I recommend this **kind**-*view*-**action** style name convention, in order to grasp what target kind is at a glance.
The argument `message` is data retrieved from the server.
The `crate-bind/build` do both create a set of dom elements and bind specific elements into a hashmap. So the `binds` will be following:

```clojure
{:el #<[object HTMLLIElement]>,
 :id-el #<[object HTMLSpanElement]>,
 :user-el #<[object HTMLSpanElement]>,
 :edit-el #<[object HTMLSpanElement]>,
 :date-el #<[object HTMLSpanElement]>,
 :body-el #<[object HTMLDivElement]>}
```

Then you can put the `binds` into the dom shelf directly.

```clojure
(swap! doms assoc-in [:message-view id] binds)
```

We use `assoc-in` here because :message-view is a collection of set of dom elements.
When a set of dom elements is singleton in your application, you can use `assoc` instead of `assoc-in` as following.

```clojure
(defn signin-view-init []
  (let [{:keys [el] :as binds}
        (crateb/build
         [:form#signin-form
          {:name "signin" :accept-charset "UTF-8"} :form-el
          [:label {:for "id"} "id or email"]
          [:input#signin-id {:type "text" :name "id"} :id-el]
          [:label {:for "password"} "password"]
          [:input#signin-password {:type "password" :name "password"} :password-el]
          [:input.submit {:type "submit" :value "Submit"} :submit-el]])]
    (dt/delegate el [["keydown" signin-view-keydown {:dom binds}]
                     ["click .submit" signin-view-submit {:dom binds}]])
    (.appendTo (js/$ el) "#wrapper")
    (swap! doms assoc :signin-view binds)))
```

The `ducttape/delegate` is a key function of Ducttape.cljs.

```clojure
(dt/delegate el [["mouseenter .message-body" message-view-popup {:dom binds}]
                 ["click .message-edit" message-view-edit-toggle {:dom binds}]])
```

```clojure
(dt/delegate el [["keydown" signin-view-keydown {:dom binds}]
                 ["click .submit" signin-view-submit {:dom binds}]])
```

All the event handlers which target this set of DOMs are bound to the root element in the same way as Backbone.js does.
First argument `el` is the root element, and second argument is actual bind settings.<br>
First element of the each vectors follows "**event-name** **selector**" form. The selector is jQuery selector.<br>
Second element is an event handler you want to bind, and third element is optional argument which is passed to the bound function.
This optional argument is very important for the event handler to identify who fires an event if it contains an id of corresponding data.

The event handlers are defined as following:

```clojure
(defn message-view-popup [{:keys [dom e]}]
  ...)

(defn message-view-edit-toggle [{:keys [dom e]}]
  ...)
```

Note that the argument is passed as a hashmap and it contains optional argument mentioned above and original event as `e`.
I recommend this optional argument style in not only event handlers but almost everywhere in your application.



Next, let's look at an example of function which get data from the server.

```clojure
(ns myapp
  (:require [simple-xhr :as sxhr]
            [ducttape :as dt]
            [crate-bind.core :as crateb]))
```

We use here [simple-xhr](https://github.com/hozumi/simple-xhr) for ajax call, but you can use your choice.

```clojure
(defn message-fetch [{:keys [id]}]
  (sxhr/request
    :url (str "/api/messages/" id)
    :method "GET"
    :complete
    (fn [xhrio]
      (when (.isSuccess xhrio)
        (let [content (-> xhrio .getResponseJson
                          (js->clj :keywordize-keys true))]
          (swap! data assoc-in [:messages id] content)
          (message-view-init {:message content}))))))
```

In the above callback, we put the data into the data shelf, and invoke corresponding view function to update ui.
Although the callback will be more complex when we fetch a collection of something, but basic procedure is the same.


### No event system
Ducttape.cljs doesn't provide additional event system intentionally.
What event system do is intelligent fucntion invocation.
Without event system, instead of firing events you need to invoke functions that update views manually after updating corresponding models, but in Ducttape.cljs updating data in the data shelf can happen within only a few (1 or maybe 2 per kind) callbacks of server request.
So manual invocation is not so painful compared with managing event.
If you really want event system, no ploblem. Put atoms into the data shelf instead of regular hashmaps, in order to `watch` all the changes of the atoms.


**Todo**
More documentation

## Installation

```clojure
[ducttape.cljs "0.1.0"]
```

## License

Distributed under the Eclipse Public License, the same as Clojure.
