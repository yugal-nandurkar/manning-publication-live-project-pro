import { View, Text, StyleSheet, ScrollView, TouchableOpacity } from 'react-native';

const ORDERS = [
  {
    id: '1',
    date: 'Oct 15, 2023',
    status: 'Delivered',
    items: [
      { name: 'Classic Potato Roll', quantity: 2 },
      { name: 'Cheese Loaded Roll', quantity: 1 },
    ],
    total: 15.97,
  },
  {
    id: '2',
    date: 'Oct 12, 2023',
    status: 'Delivered',
    items: [
      { name: 'Spicy Jalapeño Roll', quantity: 3 },
    ],
    total: 16.47,
  },
];

export default function OrdersScreen() {
  return (
    <ScrollView style={styles.container}>
      {ORDERS.map((order) => (
        <View key={order.id} style={styles.orderCard}>
          <View style={styles.orderHeader}>
            <Text style={styles.orderDate}>{order.date}</Text>
            <View style={[
              styles.statusBadge,
              { backgroundColor: order.status === 'Delivered' ? '#4CAF50' : '#FFC107' },
            ]}>
              <Text style={styles.statusText}>{order.status}</Text>
            </View>
          </View>

          <View style={styles.orderItems}>
            {order.items.map((item, index) => (
              <Text key={index} style={styles.itemText}>
                {item.quantity}x {item.name}
              </Text>
            ))}
          </View>

          <View style={styles.orderFooter}>
            <Text style={styles.totalText}>Total: ${order.total.toFixed(2)}</Text>
            <TouchableOpacity style={styles.reorderButton}>
              <Text style={styles.reorderButtonText}>Reorder</Text>
            </TouchableOpacity>
          </View>
        </View>
      ))}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f8f8f8',
    padding: 15,
  },
  orderCard: {
    backgroundColor: '#ffffff',
    borderRadius: 15,
    padding: 15,
    marginBottom: 15,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 3.84,
    elevation: 5,
  },
  orderHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 15,
  },
  orderDate: {
    fontSize: 16,
    fontWeight: '500',
    color: '#1a1a1a',
  },
  statusBadge: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 20,
  },
  statusText: {
    color: '#ffffff',
    fontSize: 12,
    fontWeight: '500',
  },
  orderItems: {
    borderTopWidth: 1,
    borderTopColor: '#f1f1f1',
    paddingVertical: 15,
  },
  itemText: {
    fontSize: 14,
    color: '#666666',
    marginBottom: 5,
  },
  orderFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    borderTopWidth: 1,
    borderTopColor: '#f1f1f1',
    paddingTop: 15,
  },
  totalText: {
    fontSize: 16,
    fontWeight: '600',
    color: '#1a1a1a',
  },
  reorderButton: {
    backgroundColor: '#d4501e',
    paddingHorizontal: 15,
    paddingVertical: 8,
    borderRadius: 20,
  },
  reorderButtonText: {
    color: '#ffffff',
    fontSize: 14,
    fontWeight: '500',
  },
});