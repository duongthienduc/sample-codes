let redis = require('redis');
let promise = require('./Promise');

const projectConfig = require('../../../../config/project.config');
let redisClient = redis.createClient(projectConfig.sharedRedisConfig);

promise.promisifyAll(redis.RedisClient.prototype);
promise.promisifyAll(redis.Multi.prototype);

module.exports = redisClient;
