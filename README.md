# Jakarta Lemon Plugin

Maven Plugin to build Jakarta EE Projects

## Main commands

### Create Data Model
`mvn jakarta-lemon:create-model`

It uses the `model.json` file to create the data model.

### Create REST Services
`mvn jakarta-lemon:create-rest`

It uses the `openapi.json` file to create the REST model.

### Create View Layer
`mvn jakarta-lemon:create-view`

It uses the `view.json` file to create the view layer.

### Add PayaraMicro Capability
`mvn jakarta-lemon:add-payara-micro`

Adds the ability to work with Payara Micro.

### Add OpenLiberty Capability
`mvn jakarta-lemon:add-openliberty`

Adds the ability to work with OpenLiberty.
