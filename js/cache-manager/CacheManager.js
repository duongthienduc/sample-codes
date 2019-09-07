let _ = require('lodash');
let MyPromise = require('./services/Promise');
let CacheClient = require('./services/CacheClient');
let logger = require('./services').Logger;

let scriptShas = {};

class CacheManager {
  /**
   * Get value by key from cache
   * @param {string} key Cache key
   * @param {object} options Options for request
   * @param {function}
   *  options.ttl Cache TTL in second
   *  options.fallback Fallback function that request for the actual data if key is not valid
   * @return {Promise}
   */
  get = (key, options) => {
    if (_.isEmpty(options) || typeof (options.fallback) !== 'function') {
      return MyPromise.reject(new Error('Please specify a fallback function in options'));
    }

    logger.debug('Get item from cache', { key });
    return CacheClient.getAsync(key)
      .then((value) => {
        if (value) {
          return JSON.parse(value);
        }

        logger.debug('Cache item not found. Trigger fallback', { key });
        return options.fallback()
          .then((fallbackValue) => {
            if (!_.isEmpty(options.ttl) && _.isInteger(options.ttl)) {
              CacheClient.set(key, JSON.stringify(fallbackValue), 'EX', options.ttl);
            } else {
              CacheClient.set(key, JSON.stringify(fallbackValue));
            }
            return fallbackValue;
          });
      });
  };

  getByScript = (script, keys = [], options = {}) => {
    if (_.isEmpty(options) || typeof (options.fallback) !== 'function') {
      return MyPromise.reject(new Error('Please specify a fallback function in options'));
    }

    logger.debug('Get item from cache', { keys });
    return this.loadScript(script)
      .then((scriptSha) => {
        let evalParams = [scriptSha, keys.length].concat(keys);
        return CacheClient.evalshaAsync(evalParams)
          .then((value) => {
            if (value) {
              return JSON.parse(value);
            }

            logger.debug('Cache item not found. Trigger fallback', { keys });
            return options.fallback()
              .then((fallbackValue )=> {
                if (options.extractCacheValues) {
                  let keyValues = fallbackValue ? options.extractCacheValues(fallbackValue) : [];
                  keyValues.forEach(entry => CacheClient.set(entry.key, JSON.stringify(entry.value)));
                }

                return fallbackValue;
              });
          });
      });
  };

  loadScript = (script) => {
    if (scriptShas[script]) {
      return MyPromise.resolve(scriptShas[script]);
    }

    logger.debug('Script SHA not found. Upload it to Redis', { script });
    return CacheClient.scriptAsync(['LOAD', script])
      .then((scriptSha) => {
        scriptShas[script] = scriptSha;
        return scriptSha;
      });
  };
}

module.exports = new CacheManager();
