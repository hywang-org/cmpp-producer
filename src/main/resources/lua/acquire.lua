local key=KEYS[1];
local ip=KEYS[2];
local maxConn=tonumber(ARGV[1]);
local totalConn=0;
local initVal='{\"conn\":1,\"lastTimestamp\":0}';
local values = redis.call("hvals", key);
if values[1] == nil then
    redis.call("hset", key, ip, initVal);
    return 1;
else
    for i,v in ipairs(values) do
        local value = cjson.decode(v);
        totalConn = value.conn + totalConn;
    end;
    if (totalConn < maxConn) then
        local value = redis.call("hget", key, ip);
        if value then
            value = cjson.decode(value);
            value.conn = value.conn + 1;
            redis.call("hset", key, ip, cjson.encode(value));
        else
            redis.call("hset", key, ip, initVal);
        end;
        return 1;
    else
        return 0;
    end;
end;