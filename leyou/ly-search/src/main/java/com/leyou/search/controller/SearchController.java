package com.leyou.search.controller;

import com.leyou.common.PageResult;
import com.leyou.pojo.Brand;
import com.leyou.pojo.Category;
import com.leyou.pojo.SpecParam;
import com.leyou.search.client.BrandClient;
import com.leyou.search.client.CategoryClient;
import com.leyou.search.client.SpecClient;
import com.leyou.search.item.Goods;
import com.leyou.search.pojo.SearchRequest;
import com.leyou.search.pojo.SearchResult;
import com.leyou.search.repository.GoodsRepository;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("search")
public class SearchController {

    @Autowired
    GoodsRepository goodsRepository;
    @Autowired
    CategoryClient categoryClient;
    @Autowired
    BrandClient brandClient;
    @Autowired
    SpecClient specClient;

    @RequestMapping("page")
    public PageResult<Goods> page(@RequestBody SearchRequest searchRequest){
        System.out.println(searchRequest.getKey()+"====="+searchRequest.getPage());

        //去es索引库做查询 or的关系
        NativeSearchQueryBuilder builder = new NativeSearchQueryBuilder();
        //构造条件在es中查询all用and的关系
        builder.withQuery(QueryBuilders.matchQuery("all",searchRequest.getKey()).operator(Operator.AND));
        //分页查询
        builder.withPageable(PageRequest.of(searchRequest.getPage()-1,searchRequest.getSize()));
        //根据新品排序
        builder.withSort(SortBuilders.fieldSort(searchRequest.getSortBy())
                .order(searchRequest.isDescending()? SortOrder.DESC:SortOrder.ASC));

        String categoryName="categoryName";
        String brandName="brandName";

        builder.addAggregation(AggregationBuilders.terms(categoryName).field("cid3"));
        builder.addAggregation(AggregationBuilders.terms(brandName).field("brandId"));

        AggregatedPage<Goods> search= (AggregatedPage<Goods>) goodsRepository.search(builder.build());
        //构造分类信息--根据分类id获取名称
        LongTerms categoryAgg= (LongTerms) search.getAggregation(categoryName);
        List<Category> categoryList=new  ArrayList<>();
        categoryAgg.getBuckets().forEach(bucket -> {
            Long categoryId = (Long) bucket.getKey();
            //根据分类id查询分类名称
            Category category = categoryClient.findCategoryById(categoryId);
            categoryList.add(category);
        });
        //构造品牌信息--根据品牌id获取名称
        LongTerms brandAgg= (LongTerms) search.getAggregation(brandName);
        List<Brand> brandList=new  ArrayList<>();
        brandAgg.getBuckets().forEach(bucket -> {
            Long brandId = (Long) bucket.getKey();
            //根据品牌id查询品牌名称
            Brand brand = brandClient.findBrandByid(brandId);
            brandList.add(brand);
        });

        //构造规格参数组数据
        List<Map<String,Object>> paramList=new ArrayList<>();
        //非空判断 -判断分类id不为空
        if(categoryList.size()==1){
            List<SpecParam> specParams = specClient.findSpecParamByCidAndSearch(categoryList.get(0).getId());
            specParams.forEach(specParam -> {
                //要分组的属性值
                String key = specParam.getName();
                builder.addAggregation(AggregationBuilders.terms(key).field("specs."+key+".keyword"));
            });
        }
        //重新查询返回数据
        AggregatedPage<Goods> search1= (AggregatedPage<Goods>) goodsRepository.search(builder.build());
        //转换map，去获取桶内属性
        Map<String, Aggregation> aggregationMap = search1.getAggregations().asMap();
        //遍历聚合结果
        aggregationMap.keySet().forEach(mKey->{
            //把品牌和分类的聚合名称排除掉
            if(!(mKey.equals(categoryName)||mKey.equals(brandName))){
                StringTerms aggregation= (StringTerms) aggregationMap.get(mKey);
                Map<String,Object> map=new HashMap<>();
                map.put("key",mKey);
                List<Map<String,String>> list=new ArrayList<>();
                aggregation.getBuckets().forEach(bucket -> {
                    Map<String,String> valueMap = new HashMap<>();
                    valueMap.put("name",bucket.getKeyAsString());
                    list.add(valueMap);//对应属性没有id，所以只填写属性，封装到options中为对象
                });
                map.put("options",list);
                //加入到获取参数的list中
                paramList.add(map);
            }
        });

        //执行查询  totalelement 总条数  content 查询内容  totalpage  总页数
        //Page<Goods> search = goodsRepository.search(builder.build());
        return  new SearchResult(search.getTotalElements(),search.getContent(),search.getTotalPages(),
                categoryList,brandList,paramList);
    }
}
