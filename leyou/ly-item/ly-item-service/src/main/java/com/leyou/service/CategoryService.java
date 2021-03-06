package com.leyou.service;

import com.leyou.dao.CategoryMapper;
import com.leyou.pojo.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {
    @Autowired
    CategoryMapper categoryMapper;

    public List<Category> findAll(Category category){
        return categoryMapper.select(category);
    }

    public void addCategory(Category category){
        categoryMapper.insertSelective(category);
    }

    public void updateCategory(Category category) {
        categoryMapper.updateByPrimaryKey(category);
    }

    public void deleteCategory(Integer id) {
        categoryMapper.deleteByPrimaryKey(id);
    }

    public Category findCategoryById(long id) {
        return categoryMapper.selectByPrimaryKey(id);
    }
}
