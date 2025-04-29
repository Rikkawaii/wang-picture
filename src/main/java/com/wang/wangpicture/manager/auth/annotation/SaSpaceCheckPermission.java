package com.wang.wangpicture.manager.auth.annotation;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import cn.hutool.core.annotation.AliasFor;
import com.wang.wangpicture.manager.auth.StpKit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 空间权限认证：必须具有指定权限才能进入该方法
 * <p> 可标注在函数、类上（效果等同于标注在此类的所有方法上）
 *
 * 基于satoken提供的注解@SaCheckPermission，增加了对空间权限的支持
 * // 写法1：使用原生注解
 * @SaCheckPermission(value = "space:edit", mode = SaMode.OR, type = StpKit.SPACE_TYPE)
 *
 * // 写法2：使用自定义注解（更简洁，且自带SPACE类型语义）
 * @SaSpaceCheckPermission(value = "space:edit", mode = SaMode.OR)
 *
 * 上面两种写法效果相同，都是要求具有空间权限“space:edit”或“admin”角色中的一个，且必须是空间类型权限。
 */
@SaCheckPermission(type = StpKit.SPACE_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface SaSpaceCheckPermission {

    /**
     * 需要校验的权限码
     *@AliasFor(annotation = SaCheckPermission.class)表示注解SaCheckPermission的value属性和本注解的value属性一致
     * @return 需要校验的权限码
     */
    @AliasFor(annotation = SaCheckPermission.class)
    String[] value() default {};

    /**
     * 验证模式：AND | OR，默认AND
     * @AliasFor(annotation = SaCheckPermission.class)表示注解SaCheckPermission的mode属性和本注解的mode属性一致
     * @return 验证模式
     */
    @AliasFor(annotation = SaCheckPermission.class)
    SaMode mode() default SaMode.AND;

    /**
     * 在权限校验不通过时的次要选择，两者只要其一校验成功即可通过校验
     *
     * <p>
     * 例1：@SaCheckPermission(value="user-add", orRole="admin")，
     * 代表本次请求只要具有 user-add权限 或 admin角色 其一即可通过校验。
     * </p>
     *
     * <p>
     * 例2： orRole = {"admin", "manager", "staff"}，具有三个角色其一即可。 <br>
     * 例3： orRole = {"admin, manager, staff"}，必须三个角色同时具备。
     * </p>
     *@AliasFor(annotation = SaCheckPermission.class)表示注解SaCheckPermission的value属性和本注解的value属性一致
     * @return /
     */
    @AliasFor(annotation = SaCheckPermission.class)
    String[] orRole() default {};

}
