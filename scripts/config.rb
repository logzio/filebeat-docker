#!/usr/bin/env ruby
require 'fileutils'
require 'pathname'
require 'yaml'
require 'erb'
require 'ostruct'

TEMPLATE = ENV["FILEBEAT_TEMPLATE"] || "/root/filebeat.yml.erb"
CONFIG = ENV["FILEBEAT_CONFIG"] || "/root/config.yml"
OUTPUT = "/filebeat.yml"

config = YAML::load_file(CONFIG)

TOKEN = config["logzio"]["token"] || ENV["LOGZIO_TOKEN"] || abort("Logzio token must be present!")

options = {
	"token" => TOKEN,
	"listener" => if config["logzio"]["listener"].nil? then "listener.logz.io"  else config["logzio"]["listener"] end,
	"port" => if config["logzio"]["port"].nil? then "5015" else config["logzio"]["port"] end,
	"files" => config['files']
}

template = File.read(TEMPLATE)
output_file = Pathname.new(OUTPUT)

File.open(output_file, 'w+') do |f|
	f.write(ERB.new(template).result(OpenStruct.new(options).instance_eval { binding }))
end
