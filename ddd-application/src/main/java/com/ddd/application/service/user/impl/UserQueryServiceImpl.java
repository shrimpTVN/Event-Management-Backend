package com.ddd.application.service.user.impl;

import com.ddd.application.service.user.UserQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserQueryServiceImpl implements UserQueryService {
}
