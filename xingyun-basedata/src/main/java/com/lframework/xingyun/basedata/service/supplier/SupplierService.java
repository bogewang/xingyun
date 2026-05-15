package com.lframework.xingyun.basedata.service.supplier;

import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.service.BaseMpService;
import com.lframework.xingyun.basedata.entity.Supplier;
import com.lframework.xingyun.basedata.vo.supplier.CreateSupplierVo;
import com.lframework.xingyun.basedata.vo.supplier.QuerySupplierSelectorVo;
import com.lframework.xingyun.basedata.vo.supplier.QuerySupplierVo;
import com.lframework.xingyun.basedata.vo.supplier.UpdateSupplierVo;
import java.util.List;

public interface SupplierService extends BaseMpService<Supplier> {

    /**
     * 查询列表
     *
     * @return
     */
    PageResult<Supplier> query(Integer pageIndex, Integer pageSize, QuerySupplierVo vo);

    /**
     * 查询列表
     *
     * @param vo
     * @return
     */
    List<Supplier> query(QuerySupplierVo vo);

    /**
     * 根据名称查询
     *
     * @param names
     * @return
     */
    List<Supplier> queryByNames(List<String> names);

    /**
     * 根据ID查询
     *
     * @param id
     * @return
     */
    Supplier findById(String id);

    /**
     * 根据ID删除（逻辑删除）
     *
     * @param id
     */
    void deleteById(String id);

    /**
     * 创建
     *
     * @param vo
     * @return
     */
    String create(CreateSupplierVo vo);

    /**
     * 生成不重复的编号
     *
     * @return
     */
    String generateCode();

    /**
     * 修改
     *
     * @param vo
     */
    void update(UpdateSupplierVo vo);

    /**
     * 选择器
     *
     * @return
     */
    PageResult<Supplier> selector(Integer pageIndex, Integer pageSize, QuerySupplierSelectorVo vo);
}
