<h2 class="alert alert-success">Congratulations, your <a class="alert-link" href="http://luminusweb.net">Luminus</a> site is ready!</h2>

This page will help guide you through the first steps of building your site.

#### Why are you seeing this page?

The `home-routes` handler in the `grownome.routes.home` namespace
defines the route that invokes the `home-page` function whenever an HTTP
request is made to the `/` URI using the `GET` method.

```
(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/docs" {:get (fn [_]
                    (-> (response/ok (-> "docs/docs.md" io/resource slurp))
                        (response/header "Content-Type" "text/plain; charset=utf-8")))}]])
```

The `home-page` function will in turn call the `grownome.layout/render` function
to render the HTML content:

```
(defn home-page [_]
  (layout/render "home.html"))
```

The page contains a link to the compiled ClojureScript found in the `target/cljsbuild/public` folder:

```
{% script "/js/app.js" %}
```

The rest of this page is rendered by ClojureScript found in the `src/cljs/grownome/core.cljs` file.



#### Organizing the routes

The routes are aggregated and wrapped with middleware in the `grownome.handler` namespace:

```
(mount/defstate app
  :start
  (middleware/wrap-base
    (ring/ring-handler
      (ring/router
        [(home-routes)])
      (ring/routes
        (ring/create-resource-handler
          {:path "/"})
        (wrap-content-type
          (wrap-webjars (constantly nil)))
        (ring/create-default-handler
          {:not-found
           (constantly (error-page {:status 404, :title "404 - Page not found"}))
           :method-not-allowed
           (constantly (error-page {:status 405, :title "405 - Not allowed"}))
           :not-acceptable
           (constantly (error-page {:status 406, :title "406 - Not acceptable"}))})))))
```

The `app` definition groups all the routes in the application into a single handler.
A default route group is added to handle the `404`, `405`, and `406` errors.

<a class="btn btn-primary" href="https://metosin.github.io/reitit/basics">learn more about routing »</a>

#### Managing your middleware

Request middleware functions are located under the `grownome.middleware` namespace.

This namespace is reserved for any custom middleware for the application. Some default middleware is
already defined here. The middleware is assembled in the `wrap-base` function.

Middleware used for development is placed in the `grownome.dev-middleware` namespace found in
the `env/dev/clj/` source path.

<a class="btn btn-primary" href="http://www.luminusweb.net/docs/middleware.md">learn more about middleware »</a>

<div class="bs-callout bs-callout-danger">

#### Database configuration is required

If you haven't already, then please follow the steps below to configure your database connection and run the necessary migrations.

* Create the database for your application.
* Update the connection URL in the `dev-config.edn` and `test-config.edn` files with your database name and login credentials.
* Run `lein run migrate` in the root of the project to create the tables.
* Let `mount` know to start the database connection by `require`-ing `grownome.db.core` in some other namespace.
* Restart the application.

<a class="btn btn-primary" href="http://www.luminusweb.net/docs/database.md">learn more about database access »</a>

</div>



#### Need some help?

Visit the [official documentation](http://www.luminusweb.net/docs) for examples
on how to accomplish common tasks with Luminus. The `#luminus` channel on the [Clojurians Slack](http://clojurians.net/) and [Google Group](https://groups.google.com/forum/#!forum/luminusweb) are both great places to seek help and discuss projects with other users.
