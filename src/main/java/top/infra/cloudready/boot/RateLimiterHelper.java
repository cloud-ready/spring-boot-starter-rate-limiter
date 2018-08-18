package top.infra.cloudready.boot;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import lombok.Setter;

import org.apache.commons.lang3.StringUtils;
import org.redisson.api.BatchOptions;
import org.redisson.api.BatchResult;
import org.redisson.api.RBatch;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

/**
 * Created by zhuowan on 2018/5/16 13:07.
 * Description:
 */

public class RateLimiterHelper {

}
