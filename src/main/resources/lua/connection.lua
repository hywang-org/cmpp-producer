local key = KEYS[1];
local conn = tonumber(ARGV[1]);
local maxConn = tonumber(redis.call("hget", key, "max_connection"));
local curConn = tonumber(redis.call("hget", key, "now_connection"));
if maxConn == nil then
    return 0;
else
    if curConn == nil then
        curConn = 0;
    end
    local newCurConn = curConn + conn;
    if newCurConn > maxConn or newCurConn < 0 then
        return 0;
    end
    redis.call("hset", key, "now_connection", curConn + conn);
    return 1;
end ;