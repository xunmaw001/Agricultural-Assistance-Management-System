
package com.controller;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.text.SimpleDateFormat;
import com.alibaba.fastjson.JSONObject;
import java.util.*;
import org.springframework.beans.BeanUtils;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.ContextLoader;
import javax.servlet.ServletContext;
import com.service.TokenService;
import com.utils.*;
import java.lang.reflect.InvocationTargetException;
import java.util.stream.Collectors;

import com.service.DictionaryService;
import org.apache.commons.lang3.StringUtils;
import com.annotation.IgnoreAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.entity.*;
import com.entity.view.*;
import com.service.*;
import com.utils.PageUtils;
import com.utils.R;
import com.alibaba.fastjson.*;

/**
 * 农产品
 * 后端接口
 * @author
 * @email
*/
@RestController
@Controller
@RequestMapping("/nongchanpin")
public class NongchanpinController {
    private static final Logger logger = LoggerFactory.getLogger(NongchanpinController.class);

    private static final String TABLE_NAME = "nongchanpin";

    @Autowired
    private NongchanpinService nongchanpinService;


    @Autowired
    private TokenService tokenService;

    @Autowired
    private DictionaryService dictionaryService;//字典
    @Autowired
    private GonggaoService gonggaoService;//助农政策
    @Autowired
    private GongzuorenyuanService gongzuorenyuanService;//工作人员
    @Autowired
    private NewsService newsService;//农产品经济数据信息
    @Autowired
    private NongchanpinXiaoshouService nongchanpinXiaoshouService;//农产品售卖
    @Autowired
    private PinkunhuService pinkunhuService;//贫困户
    @Autowired
    private SingleSeachService singleSeachService;//单页数据
    @Autowired
    private UsersService usersService;//管理员


    /**
    * 后端列表
    */
    @RequestMapping("/page")
    public R page(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("page方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));
        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永不会进入");
        else if("贫困户".equals(role))
            params.put("pinkunhuId",request.getSession().getAttribute("userId"));
        else if("工作人员".equals(role)){
            params.put("gongzuorenyuanId",request.getSession().getAttribute("userId"));

            List<PinkunhuEntity> pinkunhuEntities = pinkunhuService.selectList(new EntityWrapper<PinkunhuEntity>()
                    .eq("gongzuorenyuan_id", params.get("gongzuorenyuanId"))
            );
            if(pinkunhuEntities.size()>0){
                List<Integer> pinkunhuIds = pinkunhuEntities.stream().map(PinkunhuEntity -> PinkunhuEntity.getId()).collect(Collectors.toList());
                params.put("pinkunhuIds",pinkunhuIds);
            }
        }
        params.put("nongchanpinDeleteStart",1);params.put("nongchanpinDeleteEnd",1);
        CommonUtil.checkMap(params);
        PageUtils page = nongchanpinService.queryPage(params);

        //字典表数据转换
        List<NongchanpinView> list =(List<NongchanpinView>)page.getList();
        for(NongchanpinView c:list){
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(c, request);
        }
        return R.ok().put("data", page);
    }

    /**
    * 后端详情
    */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("info方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        NongchanpinEntity nongchanpin = nongchanpinService.selectById(id);
        if(nongchanpin !=null){
            //entity转view
            NongchanpinView view = new NongchanpinView();
            BeanUtils.copyProperties( nongchanpin , view );//把实体数据重构到view中
            //级联表 贫困户
            //级联表
            PinkunhuEntity pinkunhu = pinkunhuService.selectById(nongchanpin.getPinkunhuId());
            if(pinkunhu != null){
            BeanUtils.copyProperties( pinkunhu , view ,new String[]{ "id", "createTime", "insertTime", "updateTime", "pinkunhuId"});//把级联的数据添加到view中,并排除id和创建时间字段,当前表的级联注册表
            view.setPinkunhuId(pinkunhu.getId());
            }
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(view, request);
            return R.ok().put("data", view);
        }else {
            return R.error(511,"查不到数据");
        }

    }

    /**
    * 后端保存
    */
    @RequestMapping("/save")
    public R save(@RequestBody NongchanpinEntity nongchanpin, HttpServletRequest request){
        logger.debug("save方法:,,Controller:{},,nongchanpin:{}",this.getClass().getName(),nongchanpin.toString());

        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永远不会进入");
        else if("贫困户".equals(role))
            nongchanpin.setPinkunhuId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));

        Wrapper<NongchanpinEntity> queryWrapper = new EntityWrapper<NongchanpinEntity>()
            .eq("pinkunhu_id", nongchanpin.getPinkunhuId())
            .eq("nongchanpin_name", nongchanpin.getNongchanpinName())
            .eq("nongchanpin_address", nongchanpin.getNongchanpinAddress())
            .eq("nongchanpin_types", nongchanpin.getNongchanpinTypes())
            .eq("nongchanpin_kucun_number", nongchanpin.getNongchanpinKucunNumber())
            .eq("cangku_types", nongchanpin.getCangkuTypes())
            .eq("nongchanpin_delete", nongchanpin.getNongchanpinDelete())
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        NongchanpinEntity nongchanpinEntity = nongchanpinService.selectOne(queryWrapper);
        if(nongchanpinEntity==null){
            nongchanpin.setNongchanpinDelete(1);
            nongchanpin.setCreateTime(new Date());
            nongchanpinService.insert(nongchanpin);
            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }

    /**
    * 后端修改
    */
    @RequestMapping("/update")
    public R update(@RequestBody NongchanpinEntity nongchanpin, HttpServletRequest request) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        logger.debug("update方法:,,Controller:{},,nongchanpin:{}",this.getClass().getName(),nongchanpin.toString());
        NongchanpinEntity oldNongchanpinEntity = nongchanpinService.selectById(nongchanpin.getId());//查询原先数据

        String role = String.valueOf(request.getSession().getAttribute("role"));
//        if(false)
//            return R.error(511,"永远不会进入");
//        else if("贫困户".equals(role))
//            nongchanpin.setPinkunhuId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));
        if("".equals(nongchanpin.getNongchanpinPhoto()) || "null".equals(nongchanpin.getNongchanpinPhoto())){
                nongchanpin.setNongchanpinPhoto(null);
        }

            nongchanpinService.updateById(nongchanpin);//根据id更新
            return R.ok();
    }



    /**
    * 删除
    */
    @RequestMapping("/delete")
    public R delete(@RequestBody Integer[] ids, HttpServletRequest request){
        logger.debug("delete:,,Controller:{},,ids:{}",this.getClass().getName(),ids.toString());
        List<NongchanpinEntity> oldNongchanpinList =nongchanpinService.selectBatchIds(Arrays.asList(ids));//要删除的数据
        ArrayList<NongchanpinEntity> list = new ArrayList<>();
        for(Integer id:ids){
            NongchanpinEntity nongchanpinEntity = new NongchanpinEntity();
            nongchanpinEntity.setId(id);
            nongchanpinEntity.setNongchanpinDelete(2);
            list.add(nongchanpinEntity);
        }
        if(list != null && list.size() >0){
            nongchanpinService.updateBatchById(list);
        }

        return R.ok();
    }


    /**
     * 批量上传
     */
    @RequestMapping("/batchInsert")
    public R save( String fileName, HttpServletRequest request){
        logger.debug("batchInsert方法:,,Controller:{},,fileName:{}",this.getClass().getName(),fileName);
        Integer pinkunhuId = Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId")));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            List<NongchanpinEntity> nongchanpinList = new ArrayList<>();//上传的东西
            Map<String, List<String>> seachFields= new HashMap<>();//要查询的字段
            Date date = new Date();
            int lastIndexOf = fileName.lastIndexOf(".");
            if(lastIndexOf == -1){
                return R.error(511,"该文件没有后缀");
            }else{
                String suffix = fileName.substring(lastIndexOf);
                if(!".xls".equals(suffix)){
                    return R.error(511,"只支持后缀为xls的excel文件");
                }else{
                    URL resource = this.getClass().getClassLoader().getResource("static/upload/" + fileName);//获取文件路径
                    File file = new File(resource.getFile());
                    if(!file.exists()){
                        return R.error(511,"找不到上传文件，请联系管理员");
                    }else{
                        List<List<String>> dataList = PoiUtil.poiImport(file.getPath());//读取xls文件
                        dataList.remove(0);//删除第一行，因为第一行是提示
                        for(List<String> data:dataList){
                            //循环
                            NongchanpinEntity nongchanpinEntity = new NongchanpinEntity();
//                            nongchanpinEntity.setPinkunhuId(Integer.valueOf(data.get(0)));   // 要改的
//                            nongchanpinEntity.setNongchanpinName(data.get(0));                    //农产品名称 要改的
//                            nongchanpinEntity.setNongchanpinUuidNumber(data.get(0));                    //农产品编号 要改的
//                            nongchanpinEntity.setNongchanpinPhoto("");//详情和图片
//                            nongchanpinEntity.setNongchanpinAddress(data.get(0));                    //产出地 要改的
//                            nongchanpinEntity.setNongchanpinTypes(Integer.valueOf(data.get(0)));   //农产品类型 要改的
//                            nongchanpinEntity.setNongchanpinKucunNumber(Integer.valueOf(data.get(0)));   //农产品库存 要改的
//                            nongchanpinEntity.setNongchanpinCaigouJine(data.get(0));                    //采购价 要改的
//                            nongchanpinEntity.setNongchanpinNewJine(data.get(0));                    //销售价 要改的
//                            nongchanpinEntity.setCangkuTypes(Integer.valueOf(data.get(0)));   //所属仓库 要改的
//                            nongchanpinEntity.setNongchanpinContent("");//详情和图片
//                            nongchanpinEntity.setNongchanpinDelete(1);//逻辑删除字段
//                            nongchanpinEntity.setCreateTime(date);//时间
                            nongchanpinList.add(nongchanpinEntity);


                            //把要查询是否重复的字段放入map中
                                //农产品编号
                                if(seachFields.containsKey("nongchanpinUuidNumber")){
                                    List<String> nongchanpinUuidNumber = seachFields.get("nongchanpinUuidNumber");
                                    nongchanpinUuidNumber.add(data.get(0));//要改的
                                }else{
                                    List<String> nongchanpinUuidNumber = new ArrayList<>();
                                    nongchanpinUuidNumber.add(data.get(0));//要改的
                                    seachFields.put("nongchanpinUuidNumber",nongchanpinUuidNumber);
                                }
                        }

                        //查询是否重复
                         //农产品编号
                        List<NongchanpinEntity> nongchanpinEntities_nongchanpinUuidNumber = nongchanpinService.selectList(new EntityWrapper<NongchanpinEntity>().in("nongchanpin_uuid_number", seachFields.get("nongchanpinUuidNumber")).eq("nongchanpin_delete", 1));
                        if(nongchanpinEntities_nongchanpinUuidNumber.size() >0 ){
                            ArrayList<String> repeatFields = new ArrayList<>();
                            for(NongchanpinEntity s:nongchanpinEntities_nongchanpinUuidNumber){
                                repeatFields.add(s.getNongchanpinUuidNumber());
                            }
                            return R.error(511,"数据库的该表中的 [农产品编号] 字段已经存在 存在数据为:"+repeatFields.toString());
                        }
                        nongchanpinService.insertBatch(nongchanpinList);
                        return R.ok();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            return R.error(511,"批量插入数据异常，请联系管理员");
        }
    }




    /**
    * 前端列表
    */
    @IgnoreAuth
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("list方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));

        CommonUtil.checkMap(params);
        PageUtils page = nongchanpinService.queryPage(params);

        //字典表数据转换
        List<NongchanpinView> list =(List<NongchanpinView>)page.getList();
        for(NongchanpinView c:list)
            dictionaryService.dictionaryConvert(c, request); //修改对应字典表字段

        return R.ok().put("data", page);
    }

    /**
    * 前端详情
    */
    @RequestMapping("/detail/{id}")
    public R detail(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("detail方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        NongchanpinEntity nongchanpin = nongchanpinService.selectById(id);
            if(nongchanpin !=null){


                //entity转view
                NongchanpinView view = new NongchanpinView();
                BeanUtils.copyProperties( nongchanpin , view );//把实体数据重构到view中

                //级联表
                    PinkunhuEntity pinkunhu = pinkunhuService.selectById(nongchanpin.getPinkunhuId());
                if(pinkunhu != null){
                    BeanUtils.copyProperties( pinkunhu , view ,new String[]{ "id", "createDate"});//把级联的数据添加到view中,并排除id和创建时间字段
                    view.setPinkunhuId(pinkunhu.getId());
                }
                //修改对应字典表字段
                dictionaryService.dictionaryConvert(view, request);
                return R.ok().put("data", view);
            }else {
                return R.error(511,"查不到数据");
            }
    }


    /**
    * 前端保存
    */
    @RequestMapping("/add")
    public R add(@RequestBody NongchanpinEntity nongchanpin, HttpServletRequest request){
        logger.debug("add方法:,,Controller:{},,nongchanpin:{}",this.getClass().getName(),nongchanpin.toString());
        Wrapper<NongchanpinEntity> queryWrapper = new EntityWrapper<NongchanpinEntity>()
            .eq("pinkunhu_id", nongchanpin.getPinkunhuId())
            .eq("nongchanpin_name", nongchanpin.getNongchanpinName())
            .eq("nongchanpin_uuid_number", nongchanpin.getNongchanpinUuidNumber())
            .eq("nongchanpin_address", nongchanpin.getNongchanpinAddress())
            .eq("nongchanpin_types", nongchanpin.getNongchanpinTypes())
            .eq("nongchanpin_kucun_number", nongchanpin.getNongchanpinKucunNumber())
            .eq("cangku_types", nongchanpin.getCangkuTypes())
            .eq("nongchanpin_delete", nongchanpin.getNongchanpinDelete())
//            .notIn("nongchanpin_types", new Integer[]{102})
            ;
        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        NongchanpinEntity nongchanpinEntity = nongchanpinService.selectOne(queryWrapper);
        if(nongchanpinEntity==null){
            nongchanpin.setNongchanpinDelete(1);
            nongchanpin.setCreateTime(new Date());
        nongchanpinService.insert(nongchanpin);

            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }

}

