package com.jyh.pms.security;

import java.util.Optional;

/**
 * 认证端口：把"取用户账号"这件事与具体存储/服务实现解耦，由 service 层提供适配器实现。
 */
public interface UserAccountPort {

    Optional<UserAccount> findByUsername(String username);

    Optional<UserAccount> findByUid(String uid);
}
