package com.i.server.tabooword;

import com.i.server.data.mysql.service.dao.SmsDao;
import com.i.server.tabooword.core.TabooWordChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 作者： chengli
 * 日期： 2019/10/1 17:04
 */
@Component
public class TabooWordInitializer implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(TabooWordInitializer.class);

    @Resource
    private SmsDao smsDao;

    @Override
    public void run(String... strings) throws Exception {
        TabooWordChecker.init(smsDao);
    }
}
