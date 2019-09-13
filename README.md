## Minio Demo

We want to be able to authenticate and authorize s3 client connections over a mTLS connection. To do so we will need to install nginx, minio and run some java tests to verify that the system is working.

Warning: Please do not use this code or certificates in production as the code is  inherently insecure and only is meant for demonstrational purpouses. The reason for this is that the Java client application not is setup to verify that is is in fact talking to the correct host. To do this properly you should authorize the host with a implementation of the [javax.net.ssl.HostnameVerifier](https://www.programcreek.com/java-api-examples/?class=javax.net.ssl.HostnameVerifier&method=verify) and use the HostnameVerifier to initialize the SSLContext.

### Installation

Install MinIO: https://docs.min.io/docs/minio-quickstart-guide.html
Install NginX: https://docs.nginx.com/nginx/admin-guide/installing-nginx/installing-nginx-open-source/

### Setup NginX as a mTLS proxy
see server template: [nginx/servers/minio](./nginx/servers/minio)

The certificates and keys needed for setting up mTLS is available under [certs](./certs)

Please change the following folders in the [nginx/servers/minio](./nginx/servers/minio) so that they point to the certificates in the [certs](./certs) folder.

The following properties in [nginx/servers/minio](./nginx/servers/minio) needs to be changed:

- ssl_certificate
- ssl_certificate_key
- ssl_client_certificate

The [nginx/servers/minio](./nginx/servers/minio) needs to be moved to the nginx/servers folder so that it is loaded when restarting nginx.

After installing and starting MinIO and NginX Please verify that mTLS is working by doing the following:

    curl --verbose -X POST -d '{"someparam1":"somevalue1","someparam2":"somevalue2"}' -H "Content-Type: application/json" -k https://localhost:8092 --cert certs/client.pem --key certs/client.key

After the client and server TLS handshake you should see: `SSL connection using TLSv1.2`

### Run the tests to verify that it is working

run tests:

mvn test

Passing all the tests means you have tested the system end-to-end

You should be able to reach Minio S3 by opening https://localhost:8092, but you will need to install the [client.p12](certs/client.p12) certificate in order to be allow to access the Minio browser.
