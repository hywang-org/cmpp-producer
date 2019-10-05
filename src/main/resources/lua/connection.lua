local key = KEYS[1];
local maxConn = tonumber(redis.call("hget", key, "max_connection"));
local curConn = tonumber(redis.call("hget", key, "now_connection"));
if maxConn == nil then
    return 0;
else
    if curConn == nil then
        redis.call("hset", key, "now_connection", 1);
        return 1;
    end;
    if curConn >= maxConn then
        return 0;
    else
        redis.call("hset", key, "now_connection", curConn + 1);
        return 1;
    end;
end;