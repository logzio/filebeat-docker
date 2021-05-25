

# This project is depreacted - filebeat-docker
Docker that contains filebeat that sends logs to Logz.io

# How to use
You need to supply a configuration file, and map the logs to the container.
You can either supply the configuration file through -v, or build your own container.

## Configuration file
```yaml
---
  logzio:
    token: TOKEN
    listener: listener.logz.io

  files:
    -
      type: "mylogtype"
      logzio_codec: "plain"
      path: "/var/log/logfile.log"
      multiline:
        pattern: "^\\[?[[:digit:]]+[\\-\\/][[:digit:]]+[\\-\\/][[:digit:]]+ [[:digit:]]+:[[:digit:]]+:[[:digit:]]+"
    -
      type: "anothertype-json"
      logzio_codec: "json"
      path: "/var/log/another-json-log.log"
```
- `TOKEN` - Your logz.io token from your account settings (Can be also be Environment Variable - LOGZIO_TOKEN) 
- `listener` - The logz.io listener's address
- `type` - The type you want this log file to have (for searching in Logz.io)
- `multiline` - If you need to combine logs throughout multiple lines (i.e exceptions). More [here](https://www.elastic.co/guide/en/beats/filebeat/current/configuration-filebeat-options.html#multiline)
- The configuration file must be in `/root/config.yml` inside the container (Can be also set from Environment variable - FILEBEAT_CONFIG)

## Running the docker
```bash
docker run -d --restart=always -v /var/log:/var/log:ro -v /my/config/config.yml:/root/config.yml logzio/logzio-filebeat
```
