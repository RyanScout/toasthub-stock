package org.toasthub.stock.custom_technical_indicator;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.toasthub.utils.GlobalConstant;
import org.toasthub.utils.Request;
import org.toasthub.utils.Response;

@Repository("CustomTechnicalIndicatorDao")
@Transactional()
public class CustomTechnicalIndicatorDaoImpl implements CustomTechnicalIndicatorDao {

    @Autowired
    private EntityManager entityManager;

    @Override
    public void delete(Request request, Response response) throws Exception {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void save(Request request, Response response) throws Exception {
        entityManager.merge( (request.getParam(GlobalConstant.ITEM)) );
        
    }

    @Override
    public void items(Request request, Response response) throws Exception {
        String queryStr = "SELECT DISTINCT x FROM CustomTechnicalIndicator AS x";
        Query query = entityManager.createQuery(queryStr);
        List<?> result = query.getResultList();

        response.addParam(GlobalConstant.ITEMS, result);
    }

    @Override
    public void itemCount(Request request, Response response) throws Exception {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void item(Request request, Response response) throws Exception {
        if (request.containsParam(GlobalConstant.ITEMID) && (request.getParam(GlobalConstant.ITEMID)!= null)) {
			String queryStr = "SELECT x FROM CustomTechnicalIndicator AS x WHERE x.id =:id";
			Query query = entityManager.createQuery(queryStr);

			query.setParameter("id", new Long((Integer) request.getParam(GlobalConstant.ITEMID)));

			response.addParam(GlobalConstant.ITEM, query.getSingleResult());
        }
    }
    
}
