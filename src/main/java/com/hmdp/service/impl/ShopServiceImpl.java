package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.util.Objects;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;

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

    /**
     * 根据id查询商铺数据
     *
     * @param id
     * @return
     */
    @Override
    public Result queryById(Long id) {
        String key = CACHE_SHOP_KEY + id;
        // 1、从Redis中查询店铺数据
        String shopJson = stringRedisTemplate.opsForValue().get(key);

        Shop shop = null;
        // 2、判断缓存是否命中
        if (StrUtil.isNotBlank(shopJson)) {
            // 2.1 缓存命中，直接返回店铺数据
            shop = JSONUtil.toBean(shopJson, Shop.class);
            return Result.ok(shop);
        }
        // 2.2 缓存未命中，从数据库中查询店铺数据
        shop = this.getById(id);

        // 4、判断数据库是否存在店铺数据
        if (Objects.isNull(shop)) {
            // 4.1 数据库中不存在，返回失败信息
            return Result.fail("店铺不存在");
        }
        // 4.2 数据库中存在，写入Redis，并返回店铺数据
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop));
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