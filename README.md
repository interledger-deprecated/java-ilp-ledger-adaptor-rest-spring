# Interledger REST Ledger Adaptor (Java)

Interledger ledger adaptor for ledgers using a REST API.

Currently adheres to the API of the [Five Bells Ledger](https://github.com/interledgerjs/five-bells-ledger) with a view to support for the [Common REST API](https://github.com/interledger/rfcs/blob/2adeab20ce240302c0a30ed8473d88665319a5ed/0012-common-ledger-api/0012-common-ledger-api.md) when it is released.

## Develop

### Dependencies

The project is setup to find project dependencies in the same directory so the easiest way to work on the code is to fetch the dependencies as-is from GitHub.

```bash

    $ git checkout https://github.com/interledger/java-crypto-conditions.git
    $ git checkout https://github.com/interledger/java-ilp-core.git
    $ git checkout https://github.com/interledger/java-ilp-client.git

```

### Gradle/Maven

The project supports both Gradle and Maven build tools. A special Gradle task is defined which will generate a POM file for Maven.

```bash

    $ gradle writePom

```

### CheckStyles

The project uses Checkstyle for consitency in code style. We use the Google defined Java rules which can be configured for common IDE's by downloading configuration files from the [GitHub repo](https://github.com/google/styleguide).

## Use

The library adheres to the interfaces in the [Interledger Protocol Core](https://github.com/interledger/java-ilp-core) library. The REST adaptor is instantiated with a RestTemplateBuilder and the base URL of the ledger.

```java

    //TODO Code sample

```

The authentication details of the user can be set directly on the `RestTemplateBuilder` or passed in via `setAccountAuthToken(UsernamePasswordAuthenticationToken)`.

When `connect()` is called the adaptor will fetch the ledger meta-data and then use the data collected to establish a Websocket connection with the ledger.

## TODO

    * Fix Checkstyle issues
    * Add tests
    * Investigate async HTTP requests
    * Ensure Websocket reconnects and re-establishes subscriptions
    
## Contributors

Any contribution is very much appreciated! [![gitter][gitter-image]][gitter-url]

## License

This code is released under the Apache 2.0 License. Please see [LICENSE](LICENSE) for the full text.
