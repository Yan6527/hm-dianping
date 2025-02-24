package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    public StringRedisTemplate stringRedisTemplate;
    @Resource
    public CacheClient cacheClient;


    /**
     * 根据id查询商铺数据
     *
     * @param id
     * @return
     */
    public Result queryById(Long id) {

        //调用工具类解决缓存击穿
        Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }

    @Transactional
    @Override
    public Result updateShop(Shop shop) {
        boolean b = this.updateById(shop);
        if (!b) {
            throw new RuntimeException("数据库更新失败");
        }
        // 删除Redis缓存
        Boolean delete = stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());
        if (!delete) {
            throw new RuntimeException("删除Redis缓存失败");
        }
        return Result.ok();
    }
}